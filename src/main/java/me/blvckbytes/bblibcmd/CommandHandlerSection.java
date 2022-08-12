package me.blvckbytes.bblibcmd;

import lombok.Getter;
import me.blvckbytes.bblibconfig.AConfigSection;
import me.blvckbytes.bblibconfig.ConfigValue;
import org.bukkit.ChatColor;

import java.lang.reflect.Field;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  Contains all required messages the command handler may need.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published
  by the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
@Getter
public class CommandHandlerSection extends AConfigSection {

  private ConfigValue prefix;
  private ConfigValue internalError;
  private ConfigValue notAPlayer;
  private ConfigValue usageMismatchPrefix;
  private ConfigValue invalidDuration;
  private ConfigValue invalidFloat;
  private ConfigValue invalidInteger;
  private ConfigValue invalidEnum;
  private ConfigValue invalidUuid;
  private ConfigValue missingPermission;
  private ConfigValue offlineTarget;
  private ConfigValue unknownTarget;

  private String usageColorOther;
  private String usageColorBrackets;
  private String usageColorOptional;
  private String usageColorErrorArgFocus;
  private String usageColorMandatory;

  @Override
  public Object defaultFor(Class<?> type, String field) {
    if (type == ConfigValue.class)
      return ConfigValue.immediate("&cundefined");

    if (type == String.class)
      return "§7";

    return super.defaultFor(type, field);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    distributePrefix(prefix.asScalar(), fields);

    // Translate colors on all string fields, which will only contain colors
    for (Field f : fields) {
      if (f.getType() == String.class)
        f.set(this, ChatColor.translateAlternateColorCodes('&', (String) f.get(this)));
    }
  }
}
