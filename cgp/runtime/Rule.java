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
 * Process rule class for pCG expressions.
 *
 * David Benn, August-October 2000
 */

package cgp.runtime;

import antlr.collections.AST;
import java.util.LinkedList;

public class Rule {
    // Instance fields.
    private String name;
    private AST preconditionActionBlock, postconditionActionBlock;
    private AST[] matchExpressions, mutateKBExpressions;
    private boolean[] mutateGraphExportOpts, matchGraphNegations;
    private boolean exportAllOpt, exportAssertOpt, exportRetractOpt;
    
    // Constructors.
    public Rule(String name) {
	this.name = name;
	exportAllOpt = false;
	exportRetractOpt = false;
	exportAssertOpt = false;
	preconditionActionBlock = null;
	matchGraphNegations = null;
	matchExpressions = null;
	postconditionActionBlock = null;
	mutateGraphExportOpts = null;
	mutateKBExpressions = null;
    }

    // Public methods.
    public String getName() {
	return name;
    }

    public String toString() {
	return "";
    }

    public void setExportAllOpt(boolean truth) {
	exportAllOpt = truth;
    }

    public boolean isExportAllOpt() {
	return exportAllOpt;
    }

    public void setExportAssertOpt(boolean truth) {
	exportAssertOpt = truth;
    }

    public boolean isExportAssertOpt() {
	return exportAssertOpt;
    }

    public void setExportRetractOpt(boolean truth) {
	exportRetractOpt = truth;
    }

    public boolean isExportRetractOpt() {
	return exportRetractOpt;
    }

    public void setMatchExpressions(AST[] matchExpressions) {
	this.matchExpressions = matchExpressions;
    }

    public AST[] getMatchExpressions() {
	return matchExpressions;
    }

    public void setMatchGraphNegations(LinkedList negs) {
	matchGraphNegations = new boolean[negs.size()];
	for (int i=0;i<negs.size();i++) {
	    matchGraphNegations[i] = ((Boolean)negs.get(i)).booleanValue();
	}
    }

    /**
     * Return the ith match graph's negation status.
     * Precondition: i is within the correct range.
     */
    public boolean getMatchGraphNegation(int i) {
	return matchGraphNegations[i];
    }

    public void setMutateGraphExportOpts(LinkedList opts) {
	mutateGraphExportOpts = new boolean[opts.size()];
	for (int i=0;i<opts.size();i++) {
	    mutateGraphExportOpts[i] = ((Boolean)opts.get(i)).booleanValue();
	}
    }

    /**
     * Return the ith mutation graph's export option.
     * Precondition: i is within the correct range.
     */
    public boolean getMutateGraphExportOpt(int i) {
	return mutateGraphExportOpts[i];
    }

    public void setMutateKBExpressions(AST[] mutateKBExpressions) {
	this.mutateKBExpressions = mutateKBExpressions;
    }

    public AST[] getMutateKBExpressions() {
	return mutateKBExpressions;
    }

    public void setPreconditionActionBlock(AST preconditionActionBlock) {
	this.preconditionActionBlock = preconditionActionBlock;
    }

    public AST getPreconditionActionBlock() {
	return preconditionActionBlock;
    }

    public void setPostconditionActionBlock(AST postconditionActionBlock) {
	this.postconditionActionBlock = postconditionActionBlock;
    }

    public AST getPostconditionActionBlock() {
	return postconditionActionBlock;
    }
}
