package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;

import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.handler.NewCardActionsHandler;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// todo rename
public class CommonHandler {
    private final KeyboardProvider keyboardProvider;
    private final MessageProvider messageProvider;
    private final MessageService messageService;
    private final CollectionService collectionService;
    private final CardService cardService;
    private final NewCardActionsHandler newCardActionsHandler;

    public void collectionsScreen() {
        messageService.sendMessage(
            messageProvider.getText("emoji.collection"),
            keyboardProvider.getBackToMainMenuReply()
        );
        var page = collectionService.getCollectionsPage(getUser().getId(), 0);
        var text = messageProvider.getText("collections");

        CollectionsCallback callback = new CollectionsCallback();
        callback.setAction(CollectionCallbackAction.SELECT);
        var pageKeyboard = keyboardProvider.buildPage(page, callback);
        text = messageProvider.appendPageInfo(text, page);
        messageService.sendMessage(text, pageKeyboard);
        messageService.deleteMessagesExceptLast(2);
    }
}
