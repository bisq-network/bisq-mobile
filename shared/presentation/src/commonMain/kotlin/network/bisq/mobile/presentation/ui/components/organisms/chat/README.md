Chat UI structure

ChatScreen
  |- ChatWidget
  |    |- ChatOuterBubble (repeated for each msgs in LazyColumn)
| | |- ChatSystemMessage (in case of System message)
  |    |    |    (or)
  |    |    |- Author Timestamp
  |    |    |- QuoteMessageBubble (Only if current message quotes any prev msg)
  |    |    |    |- ChatInnerBubble
  |    |    |- Popup menu (on long press)
  |    |- Jump to bottom FAB
  |- ChatInputField