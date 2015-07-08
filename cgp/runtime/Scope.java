/**
 * A conceptual graph language which embodies Guy Mineau's process formalism.
 * Copyright (C) 2000 David Benn
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * A scope is a kind of namespace which also handles return values.
 *
 * David Benn, May-July 2000.
 */

package cgp.runtime;

import cgp.runtime.Namespace;
import cgp.runtime.Type;
import cgp.runtime.UndefinedType;

public class Scope extends Namespace {
    private Type retVal;

    public Scope() {
	super();
	retVal = UndefinedType.undefined;
    }

    public void setReturnValue(Type x) {
	retVal = x;
    }

    public Type getReturnValue() {
	return retVal;
    }
}
