package me.blvckbytes.bblibcmd;

import lombok.Getter;
import me.blvckbytes.bblibcmd.exception.*;
import me.blvckbytes.bblibconfig.IConfig;
import me.blvckbytes.bblibdi.AutoInjectLate;
import me.blvckbytes.bblibutil.TimeUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Represents the base of every command.
*/
public abstract class ACommand extends Command {

  // Arguments this command may be invoked with
  private final CommandArgument[] cmdArgs;

  // Mapping command names to their dispatchers
  private static final Map<String, ACommand> registeredCommands;

  private final CommandHandlerSection sect;

  @AutoInjectLate
  private TimeUtil timeUtil;

  // The top level permission of this command
  @Getter
  private final String rootPerm;

  static {
    registeredCommands = new HashMap<>();
  }

  /**
   * @param name Name of the command
   * @param description Description of the command
   * @param cmdArgs List of available arguments
   */
  public ACommand(
    IConfig cfg,
    String name,
    String description,
    @Nullable String rootPerm,
    CommandArgument... cmdArgs
  ) {
    super(
      // Get the name from the first entry of the comma separated list
      name.split(",")[0],
      description,

      // Generate a usage string from all first tuple items of the args-map
      Arrays.stream(cmdArgs)
        .map(CommandArgument::getName)
        .reduce("/" + name, (acc, curr) -> acc + " " + curr),

      // Get aliases by the comma separated list "name"
      // Example: <main>,<alias 1>,<alias 2>
      Arrays.stream(name.split(","))
        .skip(1)
        .map(String::trim)
        .collect(Collectors.toList())
    );

    // Set the command's permission to disallow command completion for
    // commands the player has no permission to execute
    if (rootPerm != null)
      setPermission(rootPerm);

    // Command handler config section is absolutely mandatory
    this.sect = cfg.reader("config")
      .flatMap(r -> r.parseValue("messages", CommandHandlerSection.class, true))
      .orElseThrow();

    this.cmdArgs = cmdArgs;
    this.rootPerm = rootPerm;

    // Register this command within the server's command map
    try {
      Object srv = Bukkit.getServer();
      Field cmF = srv.getClass().getDeclaredField("commandMap");
      cmF.setAccessible(true);
      Object commandMap = cmF.get(srv);

      // The register method is in CraftCommandMap's parent class, the SimpleCommandMap
      commandMap.getClass()
        .getSuperclass()
        .getDeclaredMethod("register", String.class, Command.class)
        .invoke(commandMap, name, this);
    } catch (Exception e) {
      e.printStackTrace();
    }

    registeredCommands.put(name, this);
  }

  //=========================================================================//
  //                              Overrideables                              //
  //=========================================================================//

  /**
   * Callback method for command invocations
   * @param cs Executing command sender
   * @param label Label of the command, either name or an alias
   * @param args Args passed with the command
   */
  protected abstract void invoke(CommandSender cs, String label, String[] args) throws CommandException;

  /**
   * Callback method for command autocompletion (tab)
   * @param cs Executing command sender
   * @param args Existing arguments in the chat-bar
   * @param currArg Index of the current argument within args
   * @return Stream of suggestions, will limited to 10 internally
   */
  protected abstract Stream<String> onTabCompletion(CommandSender cs, String[] args, int currArg);

  //=========================================================================//
  //                                 Command                                 //
  //=========================================================================//

  @Override
  @NotNull
  public List<String> tabComplete(
    @NotNull CommandSender sender,
    @NotNull String alias,
    @NotNull String[] args
  ) throws IllegalArgumentException {
    // Nothing to auto-complete
    if (cmdArgs.length == 0)
      return new ArrayList<>();

    // Calculate the arg index
    int currArg = Math.max(0, args.length - 1);

    // Console requested completion
    if (!(sender instanceof Player)) {
      return onTabCompletion(sender, args, currArg)
        .limit(10)
        .collect(Collectors.toList());
    }

    Player p = (Player) sender;

    // Doesn't have permission to invoke this command
    if (rootPerm != null && !p.hasPermission(rootPerm))
      return new ArrayList<>();

    // Get it's connected permission
    String argPerm = cmdArgs[Math.min(currArg, cmdArgs.length - 1)].getPermission();

    // Doesn't have permission for this arg
    if (argPerm != null && !p.hasPermission(argPerm))
      return new ArrayList<>();

    // Call tab completion handler and limit the results to 10 items
    return onTabCompletion(p, args, currArg)
      .limit(10)
      .collect(Collectors.toList());
  }

  @Override
  public boolean execute(
    @NotNull CommandSender cs,
    @NotNull String label,
    @NotNull String[] args
  ) {
    Player p = cs instanceof Player ? (Player) cs : null;

    try {
      // Check for the top level permission
      if (rootPerm != null && p != null && !p.hasPermission(rootPerm))
        throw new MissingPermissionException(sect, rootPerm);

      // Check for all permissions regarding arguments
      for (int i = 0; i < args.length; i++) {
        String argPerm = cmdArgs[Math.min(i, cmdArgs.length - 1)].getPermission();
        if (argPerm != null && p != null && !p.hasPermission(argPerm))
          throw new MissingPermissionException(sect, rootPerm);
      }

      invoke(cs, label, args);
      return true;
    }

    // Command exception occurred, send to command sender
    catch (CommandException ce) {
      cs.spigot().sendMessage(ce.getText());
      return false;
    }
  }

  //=========================================================================//
  //                            Internal Utilities                           //
  //=========================================================================//

  /////////////////////////////// Suggestions //////////////////////////////////

  /**
   * Suggest an enum's values as autocompletion, used with {@link #onTabCompletion}
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param enumClass Class of the target enum
   * @return Stream of suggestions
   */
  protected<T extends Enum<T>> Stream<String> suggestEnum(
    String[] args,
    int currArg,
    Class<T> enumClass
  ) {
    return suggestEnum(args, currArg, enumClass, (acc, curr) -> acc.add(curr.name()));
  }

  /**
   * Suggest an enum's values as autocompletion by using a custom reducer, used with {@link #onTabCompletion}
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param enumClass Class of the target enum
   * @param reducer Reducing function (acc, curr)
   * @return Stream of suggestions
   */
  protected<T extends Enum<T>> Stream<String> suggestEnum(
    String[] args,
    int currArg,
    Class<T> enumClass,
    BiConsumer<List<String>, T> reducer
  ) {
    // Collect all enum values through the reducer
    List<String> suggestions = new ArrayList<>();
    for (T c : enumClass.getEnumConstants())
      reducer.accept(suggestions, c);

    // Filter and sort the reducer's resutls
    return suggestions
      .stream()
      .sorted()
      .filter(m -> m.toLowerCase().contains(args[currArg].toLowerCase()));
  }

  /**
   * Suggest all currently online players, except the exclusion
   * @param p Invoking player
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param suggestAll Whether to suggest "all" as an option
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOnlinePlayers(Player p, String[] args, int currArg, boolean suggestAll, Player... exclude) {
    return suggestOnlinePlayers(p, args, currArg, suggestAll, List.of(exclude));
  }

  /**
   * Suggest all currently online players, except the exclusion
   * @param p Invoking player
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param suggestAll Whether to suggest "all" as an option
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOnlinePlayers(Player p, String[] args, int currArg, boolean suggestAll, List<Player> exclude) {
    Stream<? extends Player> players = Bukkit.getOnlinePlayers()
      .stream()
      .filter(o -> !exclude.contains(o))
      .filter(p::canSee);

    Stream<String> names = players
      .map(Player::getDisplayName);

    return (
      suggestAll ? Stream.concat(Stream.of("all"), names) : names
    )
      .filter(n -> n.toLowerCase().contains(args[currArg].toLowerCase()));
  }

  /**
   * Suggest all players that have ever played on this server
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOfflinePlayers(String[] args, int currArg) {
    return suggestOfflinePlayers(args, currArg, new ArrayList<>());
  }

  /**
   * Suggest all players that have ever played on this server, except the exclusion
   * @param args Already typed out arguments
   * @param currArg Currently focused argument
   * @param exclude Players to exclude from the suggestion
   * @return Stream of suggestions
   */
  protected Stream<String> suggestOfflinePlayers(String[] args, int currArg, List<OfflinePlayer> exclude) {
    return Arrays.stream(Bukkit.getOfflinePlayers())
      .filter(p -> !exclude.contains(p))
      .filter(OfflinePlayer::hasPlayedBefore)
      .map(OfflinePlayer::getName)
      .filter(Objects::nonNull)
      .filter(n -> n.toLowerCase().contains(args[currArg].toLowerCase()));
  }

  /**
   * Suggest lines of text, where each line of text has to start with the currently typed out text
   * @param args Already typed out arguments
   * @param start Index of the argument to start at
   * @param lines Lines to suggest
   * @return Stream of suggestions
   */
  protected Stream<String> suggestText(String[] args, int start, Collection<String> lines) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < args.length; i++)
      sb.append(i == 0 ? "" : " ").append(args[i].toLowerCase());

    String typed = sb.toString();
    return lines
      .stream()
      .filter(line -> line.toLowerCase().startsWith(typed));
  }

  ///////////////////////////////// Usage ////////////////////////////////////

  /**
   * Get an argument's placeholder by it's argument id (zero based index)
   * @param argId Argument id
   * @return Placeholder value
   */
  protected String getArgumentPlaceholder(int argId) {
    return cmdArgs[Math.min(argId, cmdArgs.length - 1)].getName();
  }

  /**
   * Build the usage-string in advanced mode, which supports hover tooltips
   * @param focusedArgument The argument that should be focused using the focus color
   * @return Array of components
   */
  protected BaseComponent buildUsage(@Nullable Integer focusedArgument) {
    BaseComponent head = new TextComponent(
      sect.getUsageMismatchPrefix().withPrefix() +
      sect.getUsageColorOther() + "/" + getName()
    );

    // Set the command's description as a tooltip
    head.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
      new TextComponent(sect.getUsageColorOther() + getDescription())
    }));

    // Add all it's arguments with their descriptive text as hover-tooltips
    for (int i = 0; i < this.cmdArgs.length; i++) {
      CommandArgument arg = this.cmdArgs[i];

      // Space out args
      head.addExtra(new TextComponent(" "));

      // Decide whether to colorize the argument using normal
      // colors or using the focus color based on it's positional index
      BaseComponent usage = new TextComponent(colorizeUsageString(arg.getName(), (focusedArgument != null && focusedArgument == i)));
      usage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
        new TextComponent(sect.getUsageColorOther() + arg.getDescription())
      }));

      head.addExtra(usage);
    }

    return head;
  }

  ////////////////////////////// Error Creation ////////////////////////////////

  /**
   * Generate an internal error
   */
  protected CommandException internalError() {
    return new InternalErrorException(sect);
  }

  /**
   * Generate a not a player error
   */
  protected CommandException notAPlayer() {
    return new NotAPlayerException(sect);
  }

  /**
   * Generate a usage error
   */
  protected CommandException usage() {
    throw new CommandException(buildUsage(null));
  }

  /////////////////////////// Ensure Permission /////////////////////////////

  /**
   * Ensure that the player has this permission, throw otherwise
   * @param p Target player
   * @param perm Permission to test for
   */
  protected void ensurePermission(Player p, String perm) throws CommandException {
    if (!p.hasPermission(perm))
      throw new MissingPermissionException(sect, perm);
  }

  ///////////////////////////// Parsing: Player ///////////////////////////////

  /**
   * Get an online player by their name
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   */
  protected Player onlinePlayer(String[] args, int index) throws CommandException {
    return onlinePlayer(args, index, null);
  }

  /**
   * Get an online player by their name and provide a fallback
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   */
  protected Player onlinePlayer(String[] args, int index, @Nullable Player argcFallback) throws CommandException {
    // Index out of range
    if (index >= args.length) {
      // Fallback provided
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(index));
    }

    Player target = Bukkit.getPlayerExact(args[index]);

    // The target player is not online at the moment
    if (target == null)
      throw new OfflineTargetException(sect, args[index]);

    return target;
  }

  /**
   * Get a player that has played before by their name
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   */
  protected OfflinePlayer offlinePlayer(String[] args, int index) throws CommandException {
    return offlinePlayer(args, index, null);
  }

  /**
   * Get a player that has played before by their name
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   */
  protected OfflinePlayer offlinePlayer(String[] args, int index, OfflinePlayer argcFallback) throws CommandException {
    // Index out of range
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(index));
    }

    // Find the first player that played before and has this name
    Optional<OfflinePlayer> res = Arrays.stream(Bukkit.getOfflinePlayers())
      .filter(OfflinePlayer::hasPlayedBefore)
      .filter(n -> n.getName() != null && n.getName().equals(args[index]))
      .findFirst();

    // That player has never played before
    if (res.isEmpty())
      throw new UnknownTargetException(sect, args[index]);

    return res.get();
  }

  ///////////////////////////// Parsing: Durations ///////////////////////////////

  /**
   * Parse a duration into seconds
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed duration in seconds
   */
  protected int parseDuration(String[] args, int index, Integer argcFallback) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(index));
    }

    // Time utility not available
    if (timeUtil == null)
      throw internalError();

    // Duration invalid
    int dur = timeUtil.parseDuration(args[index]);
    if (dur < 0)
      throw new InvalidDurationException(sect, args[index]);

    return dur;
  }

  ///////////////////////////// Parsing: Numbers ///////////////////////////////

  /**
   * Try to parse a floating point value from a string
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @return Parsed float
   */
  protected float parseFloat(String[] args, int index) throws CommandException {
    return parseFloat(args, index, null);
  }

  /**
   * Try to parse a floating point value from a string
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed float
   */
  protected float parseFloat(String[] args, int index, Float argcFallback) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(index));
    }

    try {
      return Float.parseFloat(args[index].replace(",", "."));
    } catch (NumberFormatException e) {
      throw new InvalidFloatException(sect, args[index]);
    }
  }

  /**
   * Try to parse an integer value from a string
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @return Parsed integer
   */
  protected int parseInt(String[] args, int index) throws CommandException {
    return parseInt(args, index, null);
  }

  /**
   * Try to parse an integer value from a string and provide a fallback
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed integer
   */
  protected int parseInt(String[] args, int index, @Nullable Integer argcFallback) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(index));
    }

    try {
      return Integer.parseInt(args[index]);
    } catch (NumberFormatException e) {
      throw new InvalidIntegerException(sect, args[index]);
    }
  }

  /**
   * Try to parse a UUID value from a string
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @return Parsed UUID
   */
  protected UUID parseUUID(String[] args, int index) throws CommandException {
    if (index >= args.length)
      throw new CommandException(buildUsage(index));

    try {
      return UUID.fromString(args[index]);
    } catch (IllegalArgumentException e) {
      throw new InvalidUuidException(sect, args[index]);
    }
  }

  ////////////////////////////// Parsing: Enum ////////////////////////////////

  /**
   * Parse an enum's value from a plain string (ignores casing) and provide a fallback
   * in case the argument count isn't sufficient to fetch the required argument
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index, T argcFallback) throws CommandException {
    return parseEnum(enumClass, args, index, argcFallback, (repr, con) -> con.name().equalsIgnoreCase(repr));
  }

  /**
   * Parse an enum's value from a plain string (ignores casing) and provide a fallback
   * in case the argument count isn't sufficient to fetch the required argument
   * @param enumClass Class of the target enum
   * @param args Arguments of the command
   * @param index Index within the arguments to use
   * @param argcFallback Fallback value to use
   * @param equalityChecker Function which checks if a enum-constant is equal to a string representation
   * @return Parsed enum value
   */
  protected<T extends Enum<T>> T parseEnum(Class<T> enumClass, String[] args, int index, T argcFallback, BiFunction<String, T, Boolean> equalityChecker) throws CommandException {
    if (index >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(index));
    }

    // Find the enum constant by it's name
    for (T constant : enumClass.getEnumConstants()) {
      // Invoke the parser
      boolean isMatch = equalityChecker.apply(args[index], constant);
      if (isMatch)
        return constant;
    }

    // Could not find any matching constants
    throw new InvalidEnumException(sect, args[index], enumClass.getEnumConstants());
  }

  /////////////////////////// Parsing: Argument spans /////////////////////////////

  /**
   * Collect a string that spans over multiple arguments
   * @param args Array of arguments
   * @param from Starting index
   * @param to Ending index
   * @param argcFallback Fallback for when the arg-count isn't sufficient
   * @return String containing space separated, joined arguments
   */
  protected String argspan(String[] args, int from, int to, @Nullable String argcFallback) throws CommandException {
    if (from >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(from));
    }

    if (to >= args.length) {
      if (argcFallback != null)
        return argcFallback;
      throw new CommandException(buildUsage(to));
    }

    StringBuilder message = new StringBuilder();

    // Loop from - to (including), append spaces to separate as needed
    for (int i = from; i <= to; i++)
      message.append(i == from ? "" : " ").append(args[i]);

    return message.toString();
  }

  /**
   * Collect a string that spans over multiple arguments till the end
   * @param args Array of arguments
   * @param from Starting index
   * @return String containing space separated, joined arguments
   */
  protected String argvar(String[] args, int from) throws CommandException {
    return argspan(args, from, args.length - 1, null);
  }

  /**
   * Collect a string that spans over multiple arguments till the end
   * @param args Array of arguments
   * @param from Starting index
   * @param argcFallback Fallback for when the arg-count isn't sufficient
   * @return String containing space separated, joined arguments
   */
  protected String argvar(String[] args, int from, String argcFallback) throws CommandException {
    return argspan(args, from, args.length - 1, argcFallback);
  }

  /**
   * Get an argument's value by index
   * @param args Array of arguments
   * @param index Index of the target argumetn
   * @return Argument's value
   */
  protected String argval(String[] args, int index) {
    return argval(args, index, null);
  }

  /**
   * Get an argument's value by index
   * @param args Array of arguments
   * @param index Index of the target argumetn
   * @param fallback Fallback string in case the argument is missing
   * @return Argument's value
   */
  protected String argval(String[] args, int index, String fallback) {
    if (index >= args.length) {
      // Fallback provided
      if (fallback != null)
        return fallback;
      throw new CommandException(buildUsage(index));
    }

    return args[index];
  }


  //=========================================================================//
  //                             Public Utilities                            //
  //=========================================================================//

  /**
   * Get an argument's description by it's argument id (zero based index)
   * @param argId Argument id
   * @return Description value
   */
  public String getArgumentDescripton(int argId) {
    return cmdArgs[Math.min(argId, cmdArgs.length - 1)].getDescription();
  }

  /**
   * Colorize the usage string based on the colors specified inside the config
   * @param vanilla Vanilla usage string
   * @param focus Whether or not to use the focus color on arguments
   * @return Colorized usage string
   */
  public String colorizeUsageString(String vanilla, boolean focus) {
    // Start out by coloring other
    StringBuilder colorized = new StringBuilder(sect.getUsageColorOther());

    /*
      - <...>     = ... Mandatory
      - [...]     = ... Optional
      - <>[]      = Brackets
      - remaining = other
     */

    // Loop individual chars
    for (char c : vanilla.toCharArray()) {
      // Colorize bracket
      if (c == '<' || c == '>' || c == '[' || c == ']') {
        colorized.append(sect.getUsageColorBrackets());
        colorized.append(c);
      }

      // Begin of mandatory
      if (c == '<') {
        colorized.append(focus ? sect.getUsageColorErrorArgFocus() : sect.getUsageColorMandatory());
      }

      // Begin of optional
      else if (c == '[') {
        colorized.append(sect.getUsageColorOptional());
      }

      // End of brackets, go back to other color
      else if (c == '>' || c == ']') {
        colorized.append(sect.getUsageColorOther());
      }

      // No bracket, normal text chars
      else
        colorized.append(c);
    }

    return colorized.toString();
  }

  //=========================================================================//
  //                             Static Utilities                            //
  //=========================================================================//

  /**
   * Get a command by it's command name string
   * @param command Command name string, casing will be ignored
   */
  public static Optional<ACommand> getByCommand(String command) {
    for (Map.Entry<String, ACommand> entry : registeredCommands.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(command))
        return Optional.of(entry.getValue());
    }

    return Optional.empty();
  }

  /**
   * Get all registered commands
   */
  public static Collection<ACommand> getCommands() {
    return registeredCommands.values();
  }
}
