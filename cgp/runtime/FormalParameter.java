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
 * A class representing a single formal parameter for pCG functions, 
 * actors, and processes.
 *
 * David Benn, July-September 2000
 */

package cgp.runtime;

public class FormalParameter {
    // Fields.
    private String name;
    private boolean out;

    // Constructors.
    public FormalParameter(String name, boolean out) {
	this.name = name;
	this.out = out;
    }

    public FormalParameter(String name) {
	this.name = name;
	this.out = true; // defaults to an "out" parameter
    }

    // Public methods.
    public String getName() {
	return name;
    }

    public boolean isIn() {
	return !out;
    }
    
    public boolean isOut() {
	return out;
    }

    public String toString() {
	String s = "";

	if (out) {
	    s += "out ";
	} else {
	    s += "in ";
	}

	s += name;

	return s;
    }    
}
