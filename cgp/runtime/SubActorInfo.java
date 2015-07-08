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
 * Sub-actor information class for pCG expressions.
 *
 * David Benn, July-September 2000
 */

package cgp.runtime;

import cgp.runtime.ConceptType;
import cgp.runtime.Type;

import java.util.LinkedList;

public class SubActorInfo {
    public Type executor;
    public LinkedList inArgs;
    public LinkedList outArgs;
    public boolean isSelfReferential;

    public SubActorInfo(Type executor, LinkedList inArgs, LinkedList outArgs,
			boolean isSelfReferential) {
	this.executor = executor;
	this.inArgs = inArgs;
	this.outArgs = outArgs;
	this.isSelfReferential = isSelfReferential;
    }

    public SubActorInfo(Type executor, LinkedList inArgs, LinkedList outArgs) {
	this(executor, inArgs, outArgs, false);
    }

    public String toString() {
	return "" + executor + ", inputs: " + inArgs + ", outputs: " + outArgs;
    }
}
