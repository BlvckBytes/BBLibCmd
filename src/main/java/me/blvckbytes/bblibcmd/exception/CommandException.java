package me.blvckbytes.bblibcmd.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.chat.BaseComponent;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Represents an exception that occurred while invoking a command.
*/
@Getter
@AllArgsConstructor
public class CommandException extends RuntimeException {

  @Getter
  private final BaseComponent text;

}
