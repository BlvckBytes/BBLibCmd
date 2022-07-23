package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

import java.util.Arrays;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The specified enum constant is an invalid option.
*/
public class InvalidEnumException extends CommandException {

  public InvalidEnumException(CommandHandlerSection sect, String input, Enum<?>[] options) {
    super(
      sect.getInvalidEnum()
        .withPrefix()
        .withVariable("input", input)
        .withVariable(
          "options",
          Arrays.stream(options)
            .map(Enum::name)
            .collect(Collectors.joining(", "))
        )
        .asComponent()
    );
  }
}
