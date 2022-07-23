package me.blvckbytes.bblibcmd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Represents an argument a command can be invoked with.
*/
@Getter
@AllArgsConstructor
public class CommandArgument {

  private String name;
  private @Nullable String permission;

  @Setter
  private String description;

  /**
   * Create a new command argument description
   * @param name Name of the argument which will be displayed, use [] fo
   *            optional and <> for mandatory arguments
   * @param permission Permission required, may be null if none
   */
  public CommandArgument(String name, @Nullable String permission) {
    this(name, "", permission);
  }

  /**
   * Get the name of this argument with stripped requirement brackets or spaces
   */
  public String getNormalizedName() {
    return name.replaceAll("[<>\\[\\] ]", "");
  }
}
