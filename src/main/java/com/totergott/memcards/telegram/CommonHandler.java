package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.QUESTION_SHOWED;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.handler.NewCardActionsHandler;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.user.TelegramUser;
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

    public void cardScreen() {
        var user = getUser();
        if (user.getFocusedOnCollection() == null) {
            cardService.getCardToLearn(user.getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(messageProvider.getMessage("no_cards", user.getLanguage()))
            );
        } else {
            cardService.getCardToLearn(user.getId(), user.getFocusedOnCollection().getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(
                    messageProvider.getMessage(
                        "no_cards_for_focus",
                        user.getLanguage(),
                        user.getFocusedOnCollection().getName()
                    )
                )
            );
        }
    }

    private void sendCard(Card card, TelegramUser user) {
        messageService.sendMessage(messageProvider.getText("emoji.card"));
        var collectionName = card.getCollection().getName();
        messageService.sendMessage(
            messageProvider.getText("emoji.collection") + collectionName,
            keyboardProvider.getBackToMainMenuReply()
        );

        var keyboard = keyboardProvider.getInlineShowAnswerKeyboard(card.getId());
        messageService.sendMessage(messageProvider.getText("emoji.card") + card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);

        messageService.deleteMessagesExceptLast(3);
    }
}
