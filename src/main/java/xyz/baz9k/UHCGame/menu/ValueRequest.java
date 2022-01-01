package xyz.baz9k.UHCGame.menu;

import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.baz9k.UHCGame.UHCGamePlugin;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

/**
 * Used to get a value from the {@link Player} once a prompting {@link ValuedNode} asks for one.
 */
public class ValueRequest {
    public enum Type {
        NUMBER_REQUEST,
        STRING_REQUEST
    }

    public ValueRequest(UHCGamePlugin plugin, Player converser, ValuedNode node) {
        this(plugin, converser, switch (node.type) {
            case INTEGER, DOUBLE -> Type.NUMBER_REQUEST;
            case STRING -> Type.STRING_REQUEST;
            default -> throw new Key("err.menu.prompt.wrong_type").transErr(IllegalArgumentException.class, node.type);
        }, node.cfgKey(), node::set, node.parent);
    }

    public ValueRequest(UHCGamePlugin plugin, Player converser, Type requestType, String requestKey, Consumer<Object> consumer) {
        this(plugin, converser, requestType, requestKey, consumer, false, null);
    }
    public ValueRequest(UHCGamePlugin plugin, Player converser, Type requestType, String requestKey, Consumer<Object> consumer, Node returnNode) {
        this(plugin, converser, requestType, requestKey, consumer, true, returnNode);
    }
    private ValueRequest(UHCGamePlugin plugin, Player converser, Type requestType, String requestKey, Consumer<Object> consumer, boolean reopenInventory, Node returnNode) {
        converser.closeInventory();

        Prompt firstPrompt = switch (requestType) {
            case NUMBER_REQUEST -> new NumberRequestPrompt();
            case STRING_REQUEST -> new StringRequestPrompt();
        };

        new ConversationFactory(plugin)
            .withInitialSessionData(Map.ofEntries(
                    Map.entry("requestKey", requestKey),
                    Map.entry("consumer", consumer),
                    Map.entry("reopenInventory", reopenInventory),
                    Map.entry("returnNode", returnNode)))
            .withTimeout(60)
            .withFirstPrompt(firstPrompt)
            .withEscapeSequence("cancel")
            .addConversationAbandonedListener(e -> {
                if (!e.gracefulExit()) {
                    Player p = (Player) e.getContext().getForWhom();
                    p.sendMessage(new Key("menu.prompt.cancel").trans());
                }
            })
            .buildConversation(converser)
            .begin();
    }

    private static class NumberRequestPrompt extends NumericPrompt {

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            Object id = context.getSessionData("requestKey");
            return renderString(new Key("menu.prompt.ask").trans(id));
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
            return renderString(new Key("menu.prompt.ask").trans(id));
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
            var requestKey       = (String) context.getSessionData("requestKey");
            var consumer         = (Consumer<Object>) context.getSessionData("consumer");
            var newValue         = (Object) context.getSessionData("newValue");
            var reopenInventory  = (boolean) context.getSessionData("reopenInventory");
            var returnNode       = (Node) context.getSessionData("returnNode");

            consumer.accept(newValue);
            if (reopenInventory) returnNode.click((Player) context.getForWhom());
            return renderString(new Key("menu.prompt.succ").trans(requestKey, newValue));
        }

        @Override
        protected @Nullable Prompt getNextPrompt(@NotNull ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }
        
    }
}
