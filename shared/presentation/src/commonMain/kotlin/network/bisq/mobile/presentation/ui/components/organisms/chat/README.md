Chat UI structure

ChatScreen
  |- ChatMessageList
  |    |- ChatRulesWarningMessageBox
  |    |- Column (has an item for each msg)
  |    |    |- ProtocolLogMessageBox
  |    |    |    (or)
  |    |    |- TradePeerLeftMessageBox
  |    |    |    (or)
  |    |    |- TextMessagebox
  |    |         |- UserNameAndDate
  |    |         |- Row (Reactions and MessageBo)
  |    |         |- ChatMessageContextMenu (on long press)
  |    |- Jump to bottom FAB
  |- ChatInputField