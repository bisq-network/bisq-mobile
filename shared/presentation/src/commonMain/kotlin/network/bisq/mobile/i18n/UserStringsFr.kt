package network.bisq.mobile.i18n

import cafe.adriel.lyricist.LyricistStrings

val FrUserStrings = UserStrings(
    user_userProfile_tooltip = "[FR] Nickname: {0}\nBot ID: {1}\nProfile ID: {2}\n{3}",
user_userProfile_tooltip_banned = "[FR] This profile is banned!",
user_userProfile_userName_banned = "[FR] [Banned] {0}",
user_userProfile_livenessState = "[FR] Last user activity: {0} ago",
user_userProfile_livenessState_ageDisplay = "[FR] {0} ago",
user_userProfile_version = "[FR] Version: {0}",
user_userProfile_addressByTransport_CLEAR = "[FR] Clear net address: {0}",
user_userProfile_addressByTransport_TOR = "[FR] Tor address: {0}",
user_userProfile_addressByTransport_I2P = "[FR] I2P address: {0}",
user_userProfile = "[FR] User profile",
user_password = "[FR] Password",
user_paymentAccounts = "[FR] Payment accounts",
user_bondedRoles_userProfile_select = "[FR] Select user profile",
user_bondedRoles_userProfile_select_invalid = "[FR] Please pick a user profile from the list",
user_userProfile_comboBox_description = "[FR] User profile",
user_userProfile_nymId = "[FR] Bot ID",
user_userProfile_nymId_tooltip = "[FR]  The 'Bot ID' is generated from the hash of the public key of that\n user profiles identity.\n It is appended to the nickname in case there are multiple user profiles in\n the network with the same nickname to distinct clearly between those profiles.",
user_userProfile_profileId = "[FR] Profile ID",
user_userProfile_profileId_tooltip = "[FR]  The 'Profile ID' is the hash of the public key of that user profiles identity\n encoded as hexadecimal string.",
user_userProfile_profileAge = "[FR] Profile age",
user_userProfile_profileAge_tooltip = "[FR] The 'Profile age' is the age in days of that user profile.",
user_userProfile_livenessState_description = "[FR] Last user activity",
user_userProfile_livenessState_tooltip = "[FR] The time passed since the user profile has been republished to the network triggered by user activity like mouse movements.",
user_userProfile_reputation = "[FR] Reputation",
user_userProfile_statement = "[FR] Statement",
user_userProfile_statement_prompt = "[FR] Enter optional statement",
user_userProfile_statement_tooLong = "[FR] Statement must not be longer than {0} characters",
user_userProfile_terms = "[FR] Trade terms",
user_userProfile_terms_prompt = "[FR] Enter optional trade terms",
user_userProfile_terms_tooLong = "[FR] Trade terms must not be longer than {0} characters",
user_userProfile_createNewProfile = "[FR] Create new profile",
user_userProfile_learnMore = "[FR] Why create a new profile?",
user_userProfile_deleteProfile = "[FR] Delete profile",
user_userProfile_deleteProfile_popup_warning = "[FR] Do you really want to delete {0}? You cannot un-do this operation.",
user_userProfile_deleteProfile_popup_warning_yes = "[FR] Yes, delete profile",
user_userProfile_deleteProfile_cannotDelete = "[FR] Deleting user profile is not permitted\n\n To delete this profile, first:\n - Delete all messages posted with this profile\n - Close all private channels for this profile\n - Make sure to have at least one more profile",
user_userProfile_popup_noSelectedProfile = "[FR] Please pick a user profile from the list",
user_userProfile_save_popup_noChangesToBeSaved = "[FR] There are no new changes to be saved",
user_userProfile_new_step2_headline = "[FR] Complete your profile",
user_userProfile_new_step2_subTitle = "[FR] You can optionally add a personalized statement to your profile and set your trade terms.",
user_userProfile_new_statement = "[FR] Statement",
user_userProfile_new_statement_prompt = "[FR] Optional add statement",
user_userProfile_new_terms = "[FR] Your trade terms",
user_userProfile_new_terms_prompt = "[FR] Optional set trade terms",
user_password_headline_setPassword = "[FR] Set password protection",
user_password_headline_removePassword = "[FR] Remove password protection",
user_password_button_savePassword = "[FR] Save password",
user_password_button_removePassword = "[FR] Remove password",
user_password_enterPassword = "[FR] Enter password (min. 8 characters)",
user_password_confirmPassword = "[FR] Confirm password",
user_password_savePassword_success = "[FR] Password protection enabled.",
user_password_removePassword_success = "[FR] Password protection removed.",
user_password_removePassword_failed = "[FR] Invalid password.",
user_paymentAccounts_headline = "[FR] Your payment accounts",
user_paymentAccounts_noAccounts_headline = "[FR] Your payment accounts",
user_paymentAccounts_noAccounts_info = "[FR] You haven't set up any accounts yet.",
user_paymentAccounts_noAccounts_whySetup = "[FR] Why is setting up an account useful?",
user_paymentAccounts_noAccounts_whySetup_info = "[FR] When you're selling Bitcoin, you need to provide your payment account  details to the buyer for receiving the fiat payment. Setting up accounts in advance allows for quick and  convenient access to this information during the trade.",
user_paymentAccounts_noAccounts_whySetup_note = "[FR] Background information:\n Your account data is exclusively stored locally on your  computer and is shared with your trade partner only when you decide to share it.",
user_paymentAccounts_accountData = "[FR] Payment account info",
user_paymentAccounts_selectAccount = "[FR] Select payment account",
user_paymentAccounts_createAccount = "[FR] Create new payment account",
user_paymentAccounts_deleteAccount = "[FR] Delete payment account",
user_paymentAccounts_createAccount_headline = "[FR] Add new payment account",
user_paymentAccounts_createAccount_subtitle = "[FR] The payment account is stored only locally on your computer and  only sent to your trade peer if you decide to do so.",
user_paymentAccounts_createAccount_accountName = "[FR] Payment account name",
user_paymentAccounts_createAccount_accountName_prompt = "[FR] Set a unique name for your payment account",
user_paymentAccounts_createAccount_accountData_prompt = "[FR] Enter the payment account info (e.g. bank account data) you  want to share with a potential Bitcoin buyer so that they can transfer you the national currency amount.",
user_paymentAccounts_createAccount_sameName = "[FR] This account name is already used. Please use a different name.",
user_profileCard_userNickname_banned = "[FR] [Banned] {0}",
user_profileCard_reputation_totalReputationScore = "[FR] Total Reputation Score",
user_profileCard_reputation_ranking = "[FR] Ranking",
user_profileCard_userActions_sendPrivateMessage = "[FR] Send private message",
user_profileCard_userActions_ignore = "[FR] Ignore",
user_profileCard_userActions_undoIgnore = "[FR] Undo ignore",
user_profileCard_userActions_report = "[FR] Report to moderator",
user_profileCard_tab_overview = "[FR] Overview",
user_profileCard_tab_details = "[FR] Details",
user_profileCard_tab_offers = "[FR] Offers ({0})",
user_profileCard_tab_reputation = "[FR] Reputation",
user_profileCard_overview_statement = "[FR] Statement",
user_profileCard_overview_tradeTerms = "[FR] Trade terms",
user_profileCard_details_botId = "[FR] Bot ID",
user_profileCard_details_userId = "[FR] User ID",
user_profileCard_details_transportAddress = "[FR] Transport address",
user_profileCard_details_totalReputationScore = "[FR] Total reputation score",
user_profileCard_details_profileAge = "[FR] Profile age",
user_profileCard_details_lastUserActivity = "[FR] Last user activity",
user_profileCard_details_version = "[FR] Software version",
user_profileCard_offers_table_columns_market = "[FR] Market",
user_profileCard_offers_table_columns_offer = "[FR] Offer",
user_profileCard_offers_table_columns_amount = "[FR] Amount",
user_profileCard_offers_table_columns_price = "[FR] Price",
user_profileCard_offers_table_columns_paymentMethods = "[FR] Payment methods",
user_profileCard_offers_table_columns_offerAge = "[FR] Age",
user_profileCard_offers_table_columns_offerAge_tooltip = "[FR] Creation date:\n{0}",
user_profileCard_offers_table_columns_goToOffer_button = "[FR] Go to offer",
user_profileCard_offers_table_placeholderText = "[FR] No offers",

    user_userProfile_payment_account = "[FR] Payment account",


)

