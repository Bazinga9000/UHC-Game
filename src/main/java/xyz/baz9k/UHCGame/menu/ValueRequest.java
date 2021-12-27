package xyz.baz9k.UHCGame.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.baz9k.UHCGame.UHCGamePlugin;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

/**
 * Used to get a value from the {@link Player} once a prompting {@link ValuedNode} asks for one.
 */
public class ValueRequest {
    public static enum Type {
        NUMBER_REQUEST,
        STRING_REQUEST
    }

    public ValueRequest(UHCGamePlugin plugin, Player converser, ValuedNode node) {
        this(plugin, converser, switch (node.type) {
            case INTEGER, DOUBLE -> Type.NUMBER_REQUEST;
            case STRING -> Type.STRING_REQUEST;
            default -> throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.menu.prompt.wrong_type", node.type);
        }, node.cfgKey(), node::set, true);
    }

    public ValueRequest(UHCGamePlugin plugin, Player converser, Type requestType, String requestKey, Consumer<Object> consumer, boolean reopenInventory) {
        var lastInventory = converser.getInventory();
        converser.closeInventory();

        Prompt firstPrompt = switch (requestType) {
            case NUMBER_REQUEST -> new NumberRequestPrompt();
            case STRING_REQUEST -> new StringRequestPrompt();
        };

        new ConversationFactory(plugin)
            .withInitialSessionData(new HashMap<>(Map.of("requestKey", requestKey, "consumer", consumer, "reopenInventory", reopenInventory, "lastInventory", lastInventory)))
            .withTimeout(60)
            .withFirstPrompt(firstPrompt)
            .withEscapeSequence("cancel")
            .addConversationAbandonedListener(e -> {
                if (!e.gracefulExit()) {
                    Player p = (Player) e.getContext().getForWhom();
                    p.sendMessage(trans("xyz.baz9k.uhc.menu.prompt.cancel"));
                }
            })
            .buildConversation(converser)
            .begin();
    }

    private static class NumberRequestPrompt extends NumericPrompt {

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            Object id = context.getSessionData("requestKey");
            return renderString(trans("xyz.baz9k.uhc.menu.prompt.ask", id));
        }
        
        @Override
        protected @Nullable Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull Number input) {
            context.setSessionData("newValue", input);
            return new SuccessMessagePrompt();
        }

    }
    private static class StringRequestPrompt extends StringPrompt {

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            Object id = context.getSessionData("requestKey");
            return renderString(trans("xyz.baz9k.uhc.menu.prompt.ask", id));
        }

        @Override
        public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
            context.setSessionData("newValue", input);
            return new SuccessMessagePrompt();
        }

    }

    private static class SuccessMessagePrompt extends MessagePrompt {

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            String requestKey = (String) context.getSessionData("requestKey");
            Consumer<Object> consumer = (Consumer<Object>) context.getSessionData("consumer");
            Object newValue = context.getSessionData("newValue");
            boolean reopenInventory = (boolean) context.getSessionData("reopenInventory");
            var lastInventory = (Inventory) context.getSessionData("lastInventory");

            consumer.accept(newValue);
            if (reopenInventory) ((Player) context.getForWhom()).openInventory(lastInventory);
            return renderString(trans("xyz.baz9k.uhc.menu.prompt.succ", requestKey, newValue));
        }

        @Override
        protected @Nullable Prompt getNextPrompt(@NotNull ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }
        
    }
}
