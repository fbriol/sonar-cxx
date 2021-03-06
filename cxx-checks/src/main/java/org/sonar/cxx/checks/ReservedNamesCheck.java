/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2017 SonarOpenCommunity
 * http://github.com/SonarOpenCommunity/sonar-cxx
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.cxx.checks;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

import com.google.common.io.Files;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.cxx.api.CxxKeyword;
import org.sonar.cxx.visitors.CxxCharsetAwareVisitor;
import org.sonar.squidbridge.checks.SquidCheck;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;

import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.cxx.tag.Tag;

@Rule(
  key = "ReservedNames",
  name = "Reserved names should not be used for preprocessor macros",
  tags = {Tag.PREPROCESSOR},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleConstantRemediation("5min")
//similar Vera++ rule T002
public class ReservedNamesCheck extends SquidCheck<Grammar> implements CxxCharsetAwareVisitor { //NOSONAR

  private static volatile String[] keywords = null;
  private Charset charset;

  @Override
  public void init() {
    keywords = CxxKeyword.keywordValues();
  }

  @Override
  public void visitFile(AstNode astNode) {
    List<String> lines;
    try {
      lines = Files.readLines(getContext().getFile(), charset);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    int nr = 0;
    for (String line : lines) {
      nr++;
      String[] sub = line.split("^\\s*#define\\s+", 2);
      if (sub.length > 1) {
        String name = sub[1].split("[\\s(]", 2)[0];
        if (name.startsWith("_") && name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
          getContext().createLineViolation(this, "Reserved name used for macro (begins with underscore followed by a capital letter)", nr);
        } else if (name.contains("__")) {
          getContext().createLineViolation(this, "Reserved name used for macro (contains two consecutive underscores)", nr);
        } else {
          name = name.toLowerCase(Locale.ENGLISH );
          for (String keyword : keywords) {
            if (name.equals(keyword)) {
              getContext().createLineViolation(this, "Reserved name used for macro (keyword or alternative token redefined)", nr);
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }
}
