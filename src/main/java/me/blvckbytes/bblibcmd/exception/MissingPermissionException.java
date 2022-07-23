package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The player is lacking a required permission to execute the command.
*/
public class MissingPermissionException extends CommandException {

  public MissingPermissionException(CommandHandlerSection sect, String permission) {
    super(
      sect.getMissingPermission()
        .withPrefix()
        .withVariable("permission", permission)
        .asComponent()
    );
  }
}
