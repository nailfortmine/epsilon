package org.kurva.werlii.client.ui.screens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.kurva.werlii.client.WerliiClient;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class AccountManagerScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nicknameField;
    private List<String> accounts = new ArrayList<>();
    private static final int MAX_NICKNAME_LENGTH = 16;
    private static final File ACCOUNTS_FILE = new File("config/werlii/accounts.json");

    public AccountManagerScreen(Screen parent) {
        super(Text.literal("Account Manager"));
        this.parent = parent;
        loadAccounts();
    }

    @Override
    protected void init() {
        // Поле для ввода ника
        nicknameField = new TextFieldWidget(
                textRenderer,
                width / 2 - 100,
                height / 4,
                200,
                20,
                Text.literal("Nickname")
        );
        nicknameField.setMaxLength(MAX_NICKNAME_LENGTH);
        nicknameField.setText(MinecraftClient.getInstance().getSession().getUsername());
        addDrawableChild(nicknameField);

        // Кнопка "Сменить ник"
        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Change Nickname"),
                                button -> changeNickname(nicknameField.getText())
                        )
                        .position(width / 2 - 100, height / 4 + 30)
                        .size(200, 20)
                        .build()
        );

        // Кнопка "Добавить аккаунт"
        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Add Account"),
                                button -> {
                                    String nickname = nicknameField.getText();
                                    if (!nickname.isEmpty() && !accounts.contains(nickname)) {
                                        accounts.add(nickname);
                                        saveAccounts();
                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§8[§bWerlii§8] §aAdded account: " + nickname)
                                        );
                                    }
                                }
                        )
                        .position(width / 2 - 100, height / 4 + 60)
                        .size(200, 20)
                        .build()
        );

        // Кнопка "Назад"
        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Back"),
                                button -> {
                                    saveAccounts();
                                    MinecraftClient.getInstance().setScreen(parent);
                                }
                        )
                        .position(width / 2 - 100, height / 4 + 90)
                        .size(200, 20)
                        .build()
        );

        // Кнопка "AcManager"
        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("AcManager"),
                                button -> {
                                    // Открывает этот же экран, можно изменить на другую логику
                                    MinecraftClient.getInstance().setScreen(new AccountManagerScreen(parent));
                                }
                        )
                        .position(width / 2 - 100, height / 4 + 120)
                        .size(200, 20)
                        .build()
        );
    }

    private void changeNickname(String newNickname) {
        if (newNickname.isEmpty() || newNickname.length() > MAX_NICKNAME_LENGTH) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("§8[§bWerlii§8] §cInvalid nickname!")
            );
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Session currentSession = client.getSession();

        Session newSession = new Session(
                newNickname,
                currentSession.getUuidOrNull(),
                currentSession.getAccessToken(),
                currentSession.getXuid(),
                currentSession.getClientId(),
                currentSession.getAccountType()
        );

        try {
            java.lang.reflect.Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(client, newSession);
            sessionField.setAccessible(false);
            client.inGameHud.getChatHud().addMessage(
                    Text.literal("§8[§bWerlii§8] §aNickname changed to: " + newNickname)
            );
        } catch (NoSuchFieldException | IllegalAccessException e) {
            client.inGameHud.getChatHud().addMessage(
                    Text.literal("§8[§bWerlii§8] §cFailed to change nickname!")
            );
            e.printStackTrace();
        }
    }

    private void loadAccounts() {
        if (ACCOUNTS_FILE.exists()) {
            try (FileReader reader = new FileReader(ACCOUNTS_FILE)) {
                accounts = new Gson().fromJson(reader, new TypeToken<List<String>>(){}.getType());
                if (accounts == null) {
                    accounts = new ArrayList<>();
                }
            } catch (Exception e) {
                // Игнорируем ошибки чтения
            }
        }
    }

    private void saveAccounts() {
        try {
            if (!ACCOUNTS_FILE.getParentFile().exists()) {
                ACCOUNTS_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(ACCOUNTS_FILE)) {
                new Gson().toJson(accounts, writer);
            }
        } catch (Exception e) {
            // Игнорируем ошибки записи
        }
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);

        for (int i = 0; i < accounts.size(); i++) {
            context.drawText(textRenderer, accounts.get(i), width / 2 - 100, height / 4 + 150 + i * 20, 0xFFFFFF, false);
        }
    }
}