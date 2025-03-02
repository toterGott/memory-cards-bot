package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.ARCHIVE;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.DELETE_DIALOG;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.EDIT_ANSWER;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.EDIT_COLLECTION;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.EDIT_QUESTION;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.EXTRACT;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.InlineKeyboardBuilder;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class CardHandler {

    protected final TextProvider textProvider;
    protected final MessageService messageService;
    protected final KeyboardProvider keyboardProvider;

    protected void printCardWithEditButtons(Card card, Callback confirmCallback) {
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
            printFinalMessage(card, confirmCallback);
        }
    }

    private void printFinalMessage(Card card, Callback confirmCallback) {
        var emoji = textProvider.get("emoji.collection");
        var cardCreatedText = textProvider.get(
            "create.card.created",
            emoji,
            card.getCollection().getName()
        );
        var id = card.getId().toString();

        String archiveText;
        Callback arcviveCallback = CreateEditCardCallback.builder()
                .data(id)
                .build();
        if (Boolean.TRUE.equals(card.getArchived())) {
            archiveText = textProvider.get("extract");
            arcviveCallback.setAction(EXTRACT.name());
        } else {
            archiveText = textProvider.get("archive");
            arcviveCallback.setAction(ARCHIVE.name());
        }

        var keyboard = new InlineKeyboardBuilder()
            .addButton(
                textProvider.get("emoji.delete")
                + textProvider.get("button.delete"),
                CreateEditCardCallback.builder().action(DELETE_DIALOG).data(id).build()
            )
            .nextRow()
            .addButton(
                textProvider.get("emoji.collection")
                + textProvider.get("button.card.edit_collection"),
                CreateEditCardCallback.builder().action(EDIT_COLLECTION).data(id).build()
            )
            .nextRow()
            .addButton(textProvider.get("button.ok"), confirmCallback)
            .nextRow()
            .addButton(archiveText, arcviveCallback)
            .build();

        messageService.sendMessage(cardCreatedText, keyboard);
    }
}
