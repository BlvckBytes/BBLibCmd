package me.blvckbytes.bblibcmd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Represents an argument a command can be invoked with.

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
    this(name, permission, "");
  }

  /**
   * Get the name of this argument with stripped requirement brackets or spaces
   */
  public String getNormalizedName() {
    return name.replaceAll("[<>\\[\\] ]", "");
  }
}
