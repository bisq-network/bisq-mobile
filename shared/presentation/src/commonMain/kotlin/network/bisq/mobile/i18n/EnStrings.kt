package network.bisq.mobile.i18n

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.EN, default = true)
val EnStrings = Strings(
    splash_details_tooltip = "Click to toggle details",
    splash_applicationServiceState_INITIALIZE_APP = "Starting Bisq",
    splash_applicationServiceState_INITIALIZE_NETWORK = "Initialize P2P network",
    splash_applicationServiceState_INITIALIZE_WALLET = "Initialize wallet",
    splash_applicationServiceState_INITIALIZE_SERVICES = "Initialize services",
    splash_applicationServiceState_APP_INITIALIZED = "Bisq started",
    splash_applicationServiceState_FAILED = "Startup failed",
    splash_bootstrapState_service_CLEAR = "Server",
    splash_bootstrapState_service_TOR = "Onion Service",
    splash_bootstrapState_service_I2P = "I2P Service",
    splash_bootstrapState_network_CLEAR = "Clear net",
    splash_bootstrapState_network_TOR = "Tor",
    splash_bootstrapState_network_I2P = "I2P",
    splash_bootstrapState_BOOTSTRAP_TO_NETWORK = { networkType -> "Bootstrap to $networkType network" },
    splash_bootstrapState_START_PUBLISH_SERVICE = "Start publishing {0}",
    splash_bootstrapState_SERVICE_PUBLISHED = "{0} published",
    splash_bootstrapState_CONNECTED_TO_PEERS = "Connecting to peers",
    tac_headline = "User Agreement",
    tac_confirm = "I have read and understood",
    tac_accept = "Accept user Agreement",
    tac_reject = "Reject and quit application",
    unlock_headline = "Enter password to unlock",
    unlock_button = "Unlock",
    unlock_failed = "Could not unlock with the provided password.\n\n Try again and be sure to have the caps lock disabled.",
    updater_headline = "A new Bisq update is available",
    updater_headline_isLauncherUpdate = "A new Bisq installer is available",
    updater_releaseNotesHeadline = "Release notes for version {0}:",
    updater_furtherInfo = "This update will be loaded after restart and does not require a new installation.\n More details can be found at the release page at:",
    updater_furtherInfo_isLauncherUpdate = "This update requires a new Bisq installation.\n If you have problems when installing Bisq on macOS, please read the instructions at:",
    updater_download = "Download and verify",
    updater_downloadLater = "Download later",
    updater_ignore = "Ignore this version",
    updater_shutDown = "Shut down",
    updater_shutDown_isLauncherUpdate = "Open download directory and shut down",
    updater_downloadAndVerify_headline = "Download and verify new version",
    updater_downloadAndVerify_info = "Once all files are downloaded, the signing key is compared with the keys provided in the  application and those available on the Bisq website. This key is then used to verify the downloaded new  version ('desktop.jar').",
    updater_downloadAndVerify_info_isLauncherUpdate = "Once all files are downloaded, the signing key is compared with the  keys provided in the application and those available on the Bisq website. This key is then used to verify the  downloaded new Bisq installer. After download and verification are complete, navigate to the download directory  to install the new Bisq version.",
    updater_table_file = "File",
    updater_table_progress = "Download progress",
    updater_table_progress_completed = "Completed",
    updater_table_verified = "Signature verified",
    notificationPanel_trades_headline_single = "New trade message for trade ''{0}''",
    notificationPanel_trades_headline_multiple = "New trade messages",
    notificationPanel_trades_button = "Go to 'Open Trades'",
    notificationPanel_mediationCases_headline_single = "New message for mediation case with trade ID ''{0}''",
    notificationPanel_mediationCases_headline_multiple = "New messages for mediation",
    notificationPanel_mediationCases_button = "Go to 'Mediator'",
    onboarding_bisq2_headline = "Welcome to Bisq 2",
    onboarding_bisq2_teaserHeadline1 = "Introducing Bisq Easy",
    onboarding_bisq2_line1 = "Getting your first Bitcoin privately\n has never been easier.",
    onboarding_bisq2_teaserHeadline2 = "Learn & discover",
    onboarding_bisq2_line2 = "Get a gentle introduction into Bitcoin\n through our guides and community chat.",
    onboarding_bisq2_teaserHeadline3 = "Coming soon",
    onboarding_bisq2_line3 = "Choose how to trade: Bisq MuSig, Lightning, Submarine Swaps,...",
    onboarding_button_create_profile = "Create profile",
    onboarding_createProfile_headline = "Create your profile",
    onboarding_createProfile_subTitle = "Your public profile consists of a nickname (picked by you) and  a bot icon (generated cryptographically)",
    onboarding_createProfile_nym = "Bot ID:",
    onboarding_createProfile_regenerate = "Generate new bot icon",
    onboarding_createProfile_nym_generating = "Calculating proof of work...",
    onboarding_createProfile_createProfile = "Next",
    onboarding_createProfile_createProfile_busy = "Initializing network node...",
    onboarding_createProfile_nickName_prompt = "Choose your nickname",
    onboarding_createProfile_nickName = "Profile nickname",
    onboarding_createProfile_nickName_tooLong = "Nickname must not be longer than {0} characters",
    onboarding_password_button_skip = "Skip",
    onboarding_password_subTitle = "Set up password protection now or skip and do it later in 'User options/Password'.",
    onboarding_password_headline_setPassword = "Set password protection",
    onboarding_password_button_savePassword = "Save password",
    onboarding_password_enterPassword = "Enter password (min. 8 characters)",
    onboarding_password_confirmPassword = "Confirm password",
    onboarding_password_savePassword_success = "Password protection enabled.",
    navigation_dashboard = "Dashboard",
    navigation_bisqEasy = "Bisq Easy",
    navigation_reputation = "Reputation",
    navigation_tradeApps = "Trade protocols",
    navigation_wallet = "Wallet",
    navigation_academy = "Learn",
    navigation_chat = "Chat",
    navigation_support = "Support",
    navigation_userOptions = "User options",
    navigation_settings = "Settings",
    navigation_network = "Network",
    navigation_authorizedRole = "Authorized role",
    navigation_expandIcon_tooltip = "Expand menu",
    navigation_collapseIcon_tooltip = "Minimize menu",
    navigation_vertical_expandIcon_tooltip = "Expand sub menu",
    navigation_vertical_collapseIcon_tooltip = "Collapse sub menu",
    navigation_network_info_clearNet = "Clear-net",
    navigation_network_info_tor = "Tor",
    navigation_network_info_i2p = "I2P",
    navigation_network_info_tooltip = "{0} network\n Number of connections: {1}\n Target connections: {2}",
    navigation_network_info_inventoryRequest_requesting = "Requesting network data",
    navigation_network_info_inventoryRequest_completed = "Network data received",
    navigation_network_info_inventoryRequests_tooltip = "Network data request state:\n Number of pending requests: {0}\n Max. requests: {1}\n All data received: {2}",
    topPanel_wallet_balance = "Balance",
    dashboard_marketPrice = "Latest market price",
    dashboard_offersOnline = "Offers online",
    dashboard_activeUsers = "Published user profiles",
    dashboard_activeUsers_tooltip = "Profiles stay published on the network\nif the user was online in the last 15 days.",
    dashboard_main_headline = "Get your first BTC",
    dashboard_main_content1 = "Start trading or browse open offers in the offerbook",
    dashboard_main_content2 = "Chat based and guided user interface for trading",
    dashboard_main_content3 = "Security is based on seller's reputation",
    dashboard_main_button = "Enter Bisq Easy",
    dashboard_second_headline = "Multiple trade protocols",
    dashboard_second_content = "Check out the roadmap for upcoming trade protocols. Get an overview about the features of the different protocols.",
    dashboard_second_button = "Explore trade protocols",
    dashboard_third_headline = "Build up reputation",
    dashboard_third_content = "You want to sell Bitcoin on Bisq Easy? Learn how the Reputation system works and why it is important.",
    dashboard_third_button = "Build Reputation",
    popup_headline_instruction = "Please note:",
    popup_headline_attention = "Attention",
    popup_headline_backgroundInfo = "Background information",
    popup_headline_feedback = "Completed",
    popup_headline_confirmation = "Confirmation",
    popup_headline_information = "Information",
    popup_headline_warning = "Warning",
    popup_headline_invalid = "Invalid input",
    popup_headline_error = "Error",
    popup_reportBug = "Report bug to Bisq developers",
    popup_reportError = "To help us to improve the software please report this bug by opening a new issue at: 'https://github.com/bisq-network/bisq2/issues'.\n The error message will be copied to the clipboard when you click the 'report' button below.\n\n It will make debugging easier if you include log files in your bug report. Log files do not contain sensitive data.",
    popup_reportBug_report = "Bisq version: {0}\n Operating system: {1}\n Error message:\n {2}",
    popup_reportError_log = "Open log file",
    popup_reportError_zipLogs = "Zip log files",
    popup_reportError_gitHub = "Report to Bisq GitHub repository",
    popup_startup_error = "An error occurred at initializing Bisq: {0}.",
    popup_shutdown = "Shut down is in process.\n\n It might take up to {0} seconds until shut down is completed.",
    popup_shutdown_error = "An error occurred at shut down: {0}.",
    popup_hyperlink_openInBrowser_tooltip = "Open link in browser: {0}.",
    popup_hyperlink_copy_tooltip = "Copy link: {0}.",
    hyperlinks_openInBrowser_attention_headline = "Open web link",
    hyperlinks_openInBrowser_attention = "Do you want to open the link to `{0}` in your default web browser?",
    hyperlinks_openInBrowser_no = "No, copy link",
    hyperlinks_copiedToClipboard = "Link was copied to clipboard",
    video_mp4NotSupported_warning_headline = "Embedded video cannot be played",
    video_mp4NotSupported_warning = "You can watch the video in your browser at: [HYPERLINK:{0}]",
    version_versionAndCommitHash = "Version: v{0} / Commit hash: {1}",

    buttons_next = "Next",
    buttons_submit = "Submit",
    buttons_cancel = "Cancel",

    common_offers = "Offers",
    common_search = "Search",
    
    offers_list_buy_from = "Buy from",
    offers_list_sell_to = "Sell to",
)