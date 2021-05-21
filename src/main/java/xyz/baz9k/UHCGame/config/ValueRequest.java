package xyz.baz9k.UHCGame.config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.baz9k.UHCGame.UHCGame;
import static xyz.baz9k.UHCGame.util.Utils.*;

/**
 * Used to get a value from the {@link Player} once a prompting {@link ValuedNode} asks for one.
 */
public class ValueRequest {
    public ValueRequest(UHCGame plugin, Player converser, ValuedNode node) {
        converser.closeInventory();

        Prompt firstPrompt = switch (node.type) {
            case INTEGER, DOUBLE -> new NumberRequestPrompt();
            case STRING -> new StringRequestPrompt();
            default -> throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.config.prompt.wrong_type", node.type);
        };

        new ConversationFactory(plugin)
            .withInitialSessionData(new HashMap<>(Map.of("id", node.id, "node", node)))
            .withTimeout(60)
            .withFirstPrompt(firstPrompt)
            .withEscapeSequence("cancel")
            .addConversationAbandonedListener(e -> {
                if (!e.gracefulExit()) {
                    Player p = (Player) e.getContext().getForWhom();
                    p.sendMessage(trans("xyz.baz9k.uhc.config.cancel"));
                }
            })
            .buildConversation(converser)
            .begin();
    }

    private static class NumberRequestPrompt extends NumericPrompt {

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            Object id = context.getSessionData("id");
            return componentString(trans("xyz.baz9k.uhc.config.ask", id));
        }
        
        @Override
        protected @Nullable Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull Number input) {
            ValuedNode node = (ValuedNode) context.getSessionData("node");
            context.setSessionData("newValue", node.restrict.apply(input));
            return new SuccessMessagePrompt();
        }

    }
    private static class StringRequestPrompt extends StringPrompt {

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            Object id = context.getSessionData("id");
            return componentString(trans("xyz.baz9k.uhc.config.prompt.ask", id));
        }

        @Override
        public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
            context.setSessionData("newValue", input);
            return new SuccessMessagePrompt();
        }

    }

    private static class SuccessMessagePrompt extends MessagePrompt {

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            String id = (String) context.getSessionData("id");
            ValuedNode node = (ValuedNode) context.getSessionData("node");
            Object newValue = context.getSessionData("newValue");

            node.set(newValue);
            node.parent.click((Player) context.getForWhom());
            return componentString(trans("xyz.baz9k.uhc.config.prompt.succ", id, newValue));
        }

        @Override
        protected @Nullable Prompt getNextPrompt(@NotNull ConversationContext context) {
            return Prompt.END_OF_CONVERSATION;
        }
        
    }
}
