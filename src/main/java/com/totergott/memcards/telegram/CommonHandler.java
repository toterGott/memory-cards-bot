package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction.EDIT_ANSWER;
import static com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction.EDIT_QUESTION;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.telegram.callback.model.NewCardCallback;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    public String getDiff(Instant appearTime, Instant now) {
        Duration duration = Duration.between(appearTime, now);
        if (duration.isNegative()) {
            duration = duration.negated();
        }

        long totalSeconds = duration.getSeconds();

        if (totalSeconds < 60) {
            return totalSeconds + " " + ChronoUnit.SECONDS;
        }

        long totalMinutes = totalSeconds / 60;
        long days = totalMinutes / (60 * 24);
        totalMinutes %= (60 * 24);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(" ").append(ChronoUnit.DAYS);
        }
        if (hours > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(hours).append(" ").append(ChronoUnit.HOURS);
        }
        if (minutes > 0 || sb.length() == 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(minutes).append(" ").append(ChronoUnit.MINUTES);
        }

        return sb.toString();
    }

    public void printCardWithEditButtons(Card card) {
        if (card.getQuestion() != null) {
            var buttonText = messageProvider.getText("button.card.edit_question");
            var callback = NewCardCallback.builder().action(EDIT_QUESTION).data(card.getId().toString()).build();

            messageService.sendMessage(
                card.getQuestion(),
                keyboardProvider.getOneInlineButton(buttonText, callback)
            );
        }
        if (card.getAnswer() != null) {
            var buttonText = messageProvider.getText("button.card.edit_answer");
            var callback = NewCardCallback.builder().action(EDIT_ANSWER).data(card.getId().toString()).build();

            messageService.sendMessage(
                card.getAnswer(),
                keyboardProvider.getOneInlineButton(buttonText, callback)
            );
        }
        if (card.getQuestion() != null && card.getAnswer() != null) {
            printFinalMessage(card);
        }
    }

    private void printFinalMessage(Card card) {
        var emoji = messageProvider.getText("emoji.collection");
        var cardCreatedText = messageProvider.getText(
            "create.card.created",
            emoji,
            card.getCollection().getName()
        );
        var cardCreatedInlineKeyboard = keyboardProvider.getCardCreatedInlineKeyboard(card.getId());
        messageService.sendMessage(cardCreatedText, cardCreatedInlineKeyboard);
    }

}
