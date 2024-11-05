package bisq.android.main;

import com.google.common.base.Joiner;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import bisq.android.AndroidApplicationService;
import bisq.android.main.user_profile.UserProfileController;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.currency.MarketRepository;
import bisq.common.encoding.Hex;
import bisq.common.locale.LanguageRepository;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.identity.NymIdGenerator;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainController {
    private final MainModel model;
    private final MainView view;
    private final AndroidApplicationService applicationService;
    private final UserProfileController userProfileController;
    private final UserIdentityService userIdentityService;

    public MainController(Path userDataDir) {
        applicationService = new AndroidApplicationService(userDataDir);
        model = new MainModel();
        view = new MainView(this, model);

        userIdentityService = applicationService.getUserService().getUserIdentityService();

        userProfileController = new UserProfileController(applicationService.getUserService(),
                applicationService.getSecurityService());
    }

    public void initialize() {
        CompletableFuture.runAsync(() -> {
            observeAppState();
            applicationService.readAllPersisted().join();
            applicationService.initialize().join();

            printDefaultKeyId();
            printLanguageCode();

            if (userIdentityService.getUserIdentities().isEmpty()) {
                //createUserIfNoneExist();
                userProfileController.initialize();

                // mock profile creation and wait until done.
                userProfileController.createUserProfile().join();
            } else {
                printUserProfiles();
            }

            observeNetworkState();
            observeNumConnections();
            printMarketPrice();

            observePrivateMessages();
            // publishRandomChatMessage();
            observeChatMessages(5);

            maybeRemoveMyOldChatMessages();
        });

        view.initialize();
    }

    public Observable<String> getLogMessage() {
        return model.getLogMessage();
    }

    // Use cases
    private void observeNetworkState() {
        Optional.ofNullable(applicationService.getNetworkService().getDefaultNodeStateByTransportType().get(TransportType.CLEAR))
                .orElseThrow()
                .addObserver(state -> appendLog("Network state", state));
    }

    private void observeNumConnections() {
        ServiceNode serviceNode = applicationService.getNetworkService().getServiceNodesByTransport().findServiceNode(TransportType.CLEAR).orElseThrow();
        Node defaultNode = serviceNode.getDefaultNode();
        PeerGroupManager peerGroupManager = serviceNode.getPeerGroupManager().orElseThrow();
        var peerGroupService = peerGroupManager.getPeerGroupService();
        AtomicLong numConnections = new AtomicLong();
        Scheduler.run(() -> {
            long currentNumConnections = peerGroupService.getAllConnectedPeers(defaultNode).count();
            if (numConnections.get() != currentNumConnections) {
                numConnections.set(currentNumConnections);
                appendLog("Number of connections", currentNumConnections);
            }
        }).periodically(100);
    }

    private void observePrivateMessages() {
        Map<String, Pin> pinByChannelId = new HashMap<>();
        applicationService.getChatService().getTwoPartyPrivateChatChannelService().getChannels()
                .addObserver(new CollectionObserver<>() {
                    @Override
                    public void add(TwoPartyPrivateChatChannel channel) {
                        appendLog("Private channel", channel.getDisplayString());
                        pinByChannelId.computeIfAbsent(channel.getId(),
                                k -> channel.getChatMessages().addObserver(new CollectionObserver<>() {
                                    @Override
                                    public void add(TwoPartyPrivateChatMessage message) {
                                        String text = "";
                                        switch (message.getChatMessageType()) {
                                            case TEXT -> {
                                                text = message.getText();
                                            }
                                            case LEAVE -> {
                                                text = "PEER LEFT " + message.getText();
                                                // leave handling not working yet correctly
                                               /* Scheduler.run(()->applicationService.getChatService().getTwoPartyPrivateChatChannelService().leaveChannel(channel))
                                                        .after(500);*/
                                            }
                                            case TAKE_BISQ_EASY_OFFER -> {
                                                text = "TAKE_BISQ_EASY_OFFER " + message.getText();
                                            }
                                            case PROTOCOL_LOG_MESSAGE -> {
                                                text = "PROTOCOL_LOG_MESSAGE " + message.getText();
                                            }
                                        }
                                        String displayString = "[" + channel.getDisplayString() + "] " + text;
                                        appendLog("Private message", displayString);
                                    }

                                    @Override
                                    public void remove(Object o) {
                                        // We do not support remove of PM
                                    }

                                    @Override
                                    public void clear() {
                                    }
                                }));
                    }

                    @Override
                    public void remove(Object o) {
                        if (o instanceof TwoPartyPrivateChatChannel channel) {
                            String id = channel.getId();
                            if (pinByChannelId.containsKey(id)) {
                                pinByChannelId.get(id).unbind();
                                pinByChannelId.remove(id);
                            }
                            appendLog("Closed private channel", channel.getDisplayString());
                        }
                    }

                    @Override
                    public void clear() {
                    }
                });
    }

    private void observeAppState() {
        applicationService.getState().addObserver(state -> appendLog("Application state", Res.get("splash.applicationServiceState." + state.name())));
    }

    private void printDefaultKeyId() {
        appendLog("Default key ID", applicationService.getSecurityService().getKeyBundleService().getDefaultKeyId());
    }

    private void printLanguageCode() {
        appendLog("Language", LanguageRepository.getDisplayLanguage(applicationService.getSettingsService().getLanguageCode().get()));
    }

    private void printMarketPrice() {
        Optional<String> priceQuote = applicationService.getBondedRolesService().getMarketPriceService()
                .findMarketPrice(MarketRepository.getUSDBitcoinMarket())
                .map(e -> MathUtils.roundDouble(e.getPriceQuote().getValue() / 10000d, 2) + " BTC/USD");
        if (priceQuote.isEmpty()) {
            Scheduler.run(this::printMarketPrice).after(500);
        } else {
            appendLog("Market price", priceQuote.get());
        }
    }

    private void createUserIfNoneExist() {
        UserIdentityService userIdentityService = applicationService.getUserService().getUserIdentityService();
        ObservableSet<UserIdentity> userIdentities = userIdentityService.getUserIdentities();
        if (userIdentities.isEmpty()) {
            String nickName = "Android " + new Random().nextInt(100);
            KeyPair keyPair = applicationService.getSecurityService().getKeyBundleService().generateKeyPair();
            byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
            ProofOfWork proofOfWork = userIdentityService.mintNymProofOfWork(pubKeyHash);
            byte[] powSolution = proofOfWork.getSolution();
            String nym = NymIdGenerator.generate(pubKeyHash, powSolution); // nym will be created on demand from pubKeyHash and pow
            // CatHash is in desktop, needs to be reimplemented or the javafx part extracted and refactored into a non javafx lib
            //  Image image = CatHash.getImage(pubKeyHash,
            //                                powSolution,
            //                                CURRENT_AVATARS_VERSION,
            //                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
            int avatarVersion = 0;
            String terms = "";
            String statement = "";
            appendLog("Create new user with", "");
            appendLog("nickName", nickName);
            appendLog("pubKeyHash", Hex.encode(pubKeyHash));
            appendLog("nym", nym);
            userIdentityService.createAndPublishNewUserProfile(nickName,
                    keyPair,
                    pubKeyHash,
                    proofOfWork,
                    avatarVersion,
                    terms,
                    statement).join();
        }
    }

    private void printUserProfiles() {
        applicationService.getUserService().getUserIdentityService().getUserIdentities().stream()
                .map(UserIdentity::getUserProfile)
                .map(userProfile -> userProfile.getUserName() + " [" + userProfile.getNym() + "]")
                .forEach(userName -> appendLog("My profile", userName));

    }

    private void publishRandomChatMessage() {
        UserService userService = applicationService.getUserService();
        UserIdentityService userIdentityService = userService.getUserIdentityService();
        ChatService chatService = applicationService.getChatService();
        ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
        CommonPublicChatChannelService discussionChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        CommonPublicChatChannel channel = discussionChannelService.getChannels().stream().findFirst().orElseThrow();
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        discussionChannelService.publishChatMessage("Dev message " + new Random().nextInt(100),
                        Optional.empty(),
                        channel,
                        userIdentity)
                .whenComplete((result, throwable) -> {
                    log.info("publishChatMessage result {}", result);
                });
    }

    private void observeChatMessages(int numLastMessages) {
        UserService userService = applicationService.getUserService();
        ChatService chatService = applicationService.getChatService();
        ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
        CommonPublicChatChannelService discussionChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        CommonPublicChatChannel channel = discussionChannelService.getChannels().stream().findFirst().orElseThrow();
        int toSkip = Math.max(0, channel.getChatMessages().size() - numLastMessages);
        List<String> displayedMessages = new ArrayList<>();
        channel.getChatMessages().stream()
                .sorted(Comparator.comparingLong(ChatMessage::getDate))
                .map(message -> {
                    displayedMessages.add(message.getId());
                    String authorUserProfileId = message.getAuthorUserProfileId();
                    String userName = userService.getUserProfileService().findUserProfile(authorUserProfileId)
                            .map(UserProfile::getUserName)
                            .orElse("N/A");
                    maybeRemoveMyOldChatMessages();
                    return "{" + userName + "} " + message.getText();
                })
                .skip(toSkip)
                .forEach(e -> appendLog("Chat message", e));

        channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void add(CommonPublicChatMessage message) {
                if (displayedMessages.contains(message.getId())) {
                    return;
                }
                displayedMessages.add(message.getId());
                String authorUserProfileId = message.getAuthorUserProfileId();
                String userName = userService.getUserProfileService().findUserProfile(authorUserProfileId)
                        .map(UserProfile::getUserName)
                        .orElse("N/A");
                String text = message.getText();
                String displayString = "{" + userName + "} " + text;
                appendLog("Chat message", displayString);
                maybeRemoveMyOldChatMessages();
            }

            @Override
            public void remove(Object o) {
                if (o instanceof CommonPublicChatMessage message)
                    appendLog("Removed chat message", message.getText());
            }

            @Override
            public void clear() {
            }
        });
    }

    private void maybeRemoveMyOldChatMessages() {
        UserService userService = applicationService.getUserService();
        UserIdentityService userIdentityService = userService.getUserIdentityService();
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        ChatService chatService = applicationService.getChatService();
        ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
        CommonPublicChatChannelService discussionChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        CommonPublicChatChannel channel = discussionChannelService.getChannels().stream().findFirst().orElseThrow();
        String myProfileId = userIdentity.getUserProfile().getId();
        appendLog("Number of chat messages", channel.getChatMessages().size());
        long expireDate = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30);
        channel.getChatMessages().stream()
                .filter(message -> message.getDate() < expireDate)
                .filter(message -> myProfileId.equals(message.getAuthorUserProfileId()))
                .forEach(message -> {
                    appendLog("Remove my old chat message", StringUtils.truncate(message.getText(), 5));
                    discussionChannelService.deleteChatMessage(message, userIdentity.getNetworkIdWithKeyPair())
                            .whenComplete((r, t) -> {
                                appendLog("Remove message result", t == null);
                                log.error(r.toString());
                            });
                });
    }

    private void appendLog(String key, Object value) {
        String line = key + ": " + value;
        List<String> logMessages = model.getLogMessages();
        logMessages.add(line);
        if (logMessages.size() > 20) {
            logMessages.remove(0);
        }
        model.getLogMessage().set(Joiner.on("\n").join(logMessages));
    }
}
