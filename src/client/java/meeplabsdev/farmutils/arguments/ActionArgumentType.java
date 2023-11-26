package meeplabsdev.farmutils.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ActionArgumentType implements ArgumentType<ActionArgument> {
    private static final Collection<String> ACTION_VALUES = Arrays.stream(ActionArgument.values()).map(s -> s.action).toList();
    public static final DynamicCommandExceptionType INVALID_ACTION = new DynamicCommandExceptionType(action -> {
        return Text.literal("Invalid action: " + action);
    });

    public static ActionArgumentType action() {
        return new ActionArgumentType();
    }

    @Override
    public ActionArgument parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readUnquotedString();

        for (ActionArgument possibleAction : ActionArgument.values()) {
            if (string.equals(possibleAction.action)) {
                return possibleAction;
            }
        }

        throw INVALID_ACTION.create(string);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(ACTION_VALUES, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return ACTION_VALUES;
    }
}
