package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.DELETE_DIALOG;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.EDIT_ANSWER;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.EDIT_COLLECTION;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.EDIT_QUESTION;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.InlineKeyboardBuilder;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction;
import com.totergott.memcards.telegram.callback.model.GetCardCallback;
import com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class CardHandler {

    protected final TextProvider textProvider;
    protected final MessageService messageService;
    protected final KeyboardProvider keyboardProvider;

    protected void printCardWithEditButtons(Card card, CallbackSource callbackSource) {
        if (card.getQuestion() != null) {
            var buttonText = textProvider.get("button.card.edit_question");
            var callback = CreateEditCardCallback.builder().action(EDIT_QUESTION).data(card.getId().toString()).build();

            messageService.sendMessage(
                card.getQuestion(),
                keyboardProvider.getOneInlineButton(buttonText, callback)
            );
        }
        if (card.getAnswer() != null) {
            var buttonText = textProvider.get("button.card.edit_answer");
            var callback = CreateEditCardCallback.builder().action(EDIT_ANSWER).data(card.getId().toString()).build();

            messageService.sendMessage(
                card.getAnswer(),
                keyboardProvider.getOneInlineButton(buttonText, callback)
            );
        }
        if (card.getQuestion() != null && card.getAnswer() != null) {
            printFinalMessage(card, callbackSource);
        }
    }

    private void printFinalMessage(Card card, CallbackSource callbackSource) {
        var emoji = textProvider.get("emoji.collection");
        var cardCreatedText = textProvider.get(
            "create.card.created",
            emoji,
            card.getCollection().getName()
        );
        var id = card.getId().toString();
        var breadcrumb = callbackSource.getCode();
        var confirmCallback = switch (callbackSource) {
            case CallbackSource.GET_CARD -> GetCardCallback.builder().action(GetCardCallbackAction.OK_AFTER_EDIT).build();
            case CallbackSource.NEW_CARD -> CreateEditCardCallback.builder().action(CreateEditCardCallbackAction.CONFIRM).build();
            default -> throw new IllegalStateException("Unexpected value: " + callbackSource);
        };
        confirmCallback.setData(card.getId().toString());
        confirmCallback.setAdditionalData(breadcrumb); // todo is it really needed?


        var keyboard = new InlineKeyboardBuilder()
            .addButton(
                textProvider.get("emoji.delete")
                + textProvider.get("button.card.delete"),
                CreateEditCardCallback.builder().action(DELETE_DIALOG).data(id).additionalData(breadcrumb).build()
            )
            .addButton(
                textProvider.get("emoji.collection")
                + textProvider.get("button.card.edit_collection"),
                CreateEditCardCallback.builder().action(EDIT_COLLECTION).data(id).build()
            )
            .addRow()
            .addButton(textProvider.get("button.ok"), confirmCallback)
            .build();

        messageService.sendMessage(cardCreatedText, keyboard);
    }
}
