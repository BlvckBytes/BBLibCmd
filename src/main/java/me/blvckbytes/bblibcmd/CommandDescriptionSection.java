package me.blvckbytes.bblibcmd;

import lombok.Getter;
import me.blvckbytes.bblibconfig.AConfigSection;
import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibconfig.sections.CSMap;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  Contains all values required to add descriptions to a
  command itself and all of it's arguments.
*/
@Getter
public class CommandDescriptionSection extends AConfigSection {

  private ConfigValue description;

  @CSMap(k = String.class, v = ConfigValue.class)
  private Map<String, ConfigValue> args;

}
