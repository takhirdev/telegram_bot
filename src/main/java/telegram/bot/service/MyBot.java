package telegram.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import telegram.bot.enity.UserGroupEntity;
import telegram.bot.repository.UserGroupRepository;

import java.util.*;


@Component
public class MyBot extends TelegramLongPollingBot {
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.username}")
    private String userName;

    @Override
    public String getBotUsername() {
        return userName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private static final String HELP_MESSAGE = "Bu bot siz yuborgan xabarni belgilangan guruhlarga forward qiladi!";
    private static final String ADD_GROUP = "ADD_GROUP";
    private static final String LIST_GROUPS = "LIST_GROUPS";
    private Map<String, String> userSelectedGroup = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();
            String hello = "Assalomu aleykum " + update.getMessage().getFrom().getFirstName() + ". Botga xush kelibsz!";

            switch (text) {
                case "/start":
                    sendBackButton(chatId, hello);
                    startCommandReceivedBot(chatId);
                    userSelectedGroup.clear();
                    break;
                case "/help":
                    sendMsg(chatId, HELP_MESSAGE);
                    break;
                case "Orqaga":
                    startCommandReceivedBot(chatId);
                    break;
                default:
                    handleUserInput(update, chatId, text);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleUserInput(Update update, String chatId, String text) {
        if (text.startsWith("@")) {
            Optional<UserGroupEntity> optional = userGroupRepository.findByGroupUserNameAndChatId(text, chatId);
            if (optional.isPresent()) {
                sendMsg(chatId, "Siz bu guruhni oldin qo'shgansiz");
                return;
            }

            UserGroupEntity entity = new UserGroupEntity();
            entity.setChatId(chatId);
            entity.setFirstName(update.getMessage().getFrom().getFirstName());
            entity.setLastName(update.getMessage().getFrom().getLastName());
            entity.setUserName(update.getMessage().getFrom().getUserName());
            entity.setGroupName(text);
            userGroupRepository.save(entity);

            sendMsg(chatId, "Telegram guruh muvaffaqiyatli qo'shildi!");
        } else {
            String selectedGroup = userSelectedGroup.get(chatId);
            if (selectedGroup == null) {
                sendMsg(chatId, "Iltimos, avval guruh tanlang.");
                return;
            }
            sendFormattingMsg(selectedGroup, text, chatId);
        }
    }


    private void handleCallbackQuery(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String data = update.getCallbackQuery().getData();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        switch (data) {
            case ADD_GROUP:
                sendEditMsg(chatId, messageId, "Telegram guruh username ni kiriting ( @testGroup)");
                break;
            case LIST_GROUPS:
                List<UserGroupEntity> groupList = userGroupRepository.findAllByChatId(chatId);
                if (groupList.isEmpty()) {
                    sendEditMsg(chatId, messageId, "Hozirda sizda Telegram guruhlari mavjud emas!");
                    return;
                }
                StringBuilder sendText = new StringBuilder("Xabar yubormoqchi bo'lgan guruhni tanlang!\n\n");
                for (int i = 0; i < groupList.size(); i++) {
                    sendText.append(i + 1).append(". ").append(groupList.get(i).getGroupName()).append("\n");
                }
                sendInlineKeyboardMarkup(chatId, messageId, groupList, sendText.toString(), groupList.size());
                break;
            default:

                userSelectedGroup.put(chatId, data);  // user tanlagan guruhni tanlangan guruhlar royhatiga qo'shish

                sendEditMsg(chatId, messageId, data + " guruhi tanlandi. Xabar yozishni boshlang");
                break;
        }
    }

    private void sendInlineKeyboardMarkup(String chatId, Integer messageId, List<UserGroupEntity> groupList, String text, Integer buttonCounts) {
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);
        sendMessage.setMessageId(messageId);
        sendMessage.setText(text);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowCollection = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (int i = 0; i < buttonCounts; i++) {    //   user qo'shgan guruhlari soniga qarab button yaratadi
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(String.valueOf(i + 1));
            button.setCallbackData(groupList.get(i).getGroupName());
            row.add(button);
        }

        rowCollection.add(row);
        inlineKeyboardMarkup.setKeyboard(rowCollection);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void startCommandReceivedBot(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Tanlang:\n");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowCollection = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Telegram guruh qo'shish");
        addButton.setCallbackData(ADD_GROUP);

        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("Mening telegram guruhlarim");
        listButton.setCallbackData(LIST_GROUPS);

        row1.add(listButton);
        row2.add(addButton);

        rowCollection.add(row1);
        rowCollection.add(row2);

        inlineKeyboardMarkup.setKeyboard(rowCollection);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        clearUserSelectedGroup(chatId);   //  Start yoki Orqaga buyrug'i bosilganda user tanlagan guruhni
                                          //  tanlangan guruhlar ro'yhatidan o'chirish
    }

    private void sendBackButton(String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        KeyboardButton backButton = new KeyboardButton();
        backButton.setText("Orqaga");
        row.add(backButton);

        keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(true);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendEditMsg(String chatId, Integer messageId, String newMessage) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(newMessage);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendFormattingMsg(String groupUserName, String message, String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(groupUserName);
        sendMessage.setText("```\n" + message + "\n```");
        sendMessage.setParseMode("Markdown");
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            sendMsg(chatId, """
                    Bu guruhga habar yuborish uchun botga ruhsat yo'q.
                    Bot xabar yubora olishi uchun guruhda admin bo'lishi kerak.
                    Mazgi bot statusini ADMIN ga o'zgartir va qayta urinib ko'r!!!!
                    """);
        }
    }

    private void clearUserSelectedGroup(String chatId) {
        userSelectedGroup.remove(chatId);
    }
}
