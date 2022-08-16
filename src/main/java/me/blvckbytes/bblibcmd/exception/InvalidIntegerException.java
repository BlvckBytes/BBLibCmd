package me.blvckbytes.bblibcmd.exception;

import me.blvckbytes.bblibcmd.CommandHandlerSection;
import me.blvckbytes.bblibutil.component.GradientGenerator;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  The specified integer is malformed.

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
public class InvalidIntegerException extends CommandException {

  public InvalidIntegerException(CommandHandlerSection sect, String input, @Nullable GradientGenerator gradientGenerator) {
    super(
      sect.getInvalidDuration()
        .withPrefix()
        .withVariable("input", input)
        .asComponent(gradientGenerator)
    );
  }
}
