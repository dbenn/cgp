/**
 * A conceptual graph language which embodies Guy Mineau's process formalism.
 * Copyright (C) 2000,2001 David Benn
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
 * File type class for pCG expressions.
 *
 * David Benn, August 2000, June 2001
 */

package cgp.runtime;

import cgp.runtime.GraphType;
import cgp.runtime.ListType;
import cgp.runtime.Type;
import cgp.runtime.UndefinedType;
import cgp.translators.CGIFParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import notio.Graph;
import notio.ParserException;
import notio.TranslationContext;

public class FileType extends Type {
    // Fields.
    private String origPath;
    private String path;
    private File file;
    private BufferedReader reader;
    private BufferedWriter writer;

    // Constructors.
    public FileType(String path) throws IOException {
	origPath = path;
	if (path.startsWith("<")) {
	    this.path = path.substring(1);
	    file = new File(this.path);
	    reader = new BufferedReader(new FileReader(file));
	} else if (path.startsWith(">")) {
	    this.path = path.substring(1);
	    file = new File(this.path);
	    writer = new BufferedWriter(new FileWriter(file));
	} else {
	    this.path = path;
	    file = new File(this.path);
	    reader =  new BufferedReader(new FileReader(file));
	}

	setType("file");
    }

    // Public methods.

    public File getValue() {
	return file;
    }

    public String toString() {
	String str = path + "; opened for ";
	str += writer == null ? "reading" : "writing";
	return str;
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for pCG attributes.
     */

    public StringType getKind() {
	return new StringType(writer == null ? "reader" : "writer");
    }
 
    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */

    public Type readline() throws IOException {
	Type result = UndefinedType.undefined;

	if (reader != null) {
	    result = new StringType(reader.readLine());
	} else {
	    throw new IOException("cannot read from " + this);
	}

	return result;
    }

    public Type readall() throws IOException {
	Type result = UndefinedType.undefined;
	
	if (reader != null) {
	    ListType linesList = new ListType();
	    while (reader.ready()) {
		linesList.append(new StringType(reader.readLine()));
	    }
	    result = linesList;
	} else {
	    throw new IOException("cannot read from " + this);
	}

	return result;
    }

    public Type readGraph() throws IOException {
	Type result = UndefinedType.undefined;

	if (reader != null) {
	    result = new GraphType(file);
	} else {
	    throw new IOException("cannot read from " + this);
	}

	return result;
    }

    public Type readGraphStream() throws IOException, ParserException {
	Type result = UndefinedType.undefined;

	if (reader != null) {
	    CGIFParser parser = new CGIFParser();
	    parser.initializeParser(file, Type.getKBStack().peek().getKB(),
				    new TranslationContext());
	    Graph[] graphs = parser.parseGraphStream();
	    ListType gList = new ListType();
	    for (int i=0;i<graphs.length;i++) {
		gList.append(new GraphType(graphs[i]));
	    }
	    result = gList;
	} else {
	    throw new IOException("cannot read from " + this);
	}

	return result;
    }

    public void write(StringType s) throws IOException {
	if (writer != null) {
	    String t = s.getValue();
	    writer.write(t,0,t.length());
	} else {
	    throw new IOException("cannot write to " + this);
	}
    }

    public void writeln(StringType s) throws IOException {
	if (writer != null) {
	    String t = s.getValue();
	    writer.write(t,0,t.length());
	    writer.newLine();
	} else {
	    throw new IOException("cannot write to " + this);
	}
    }

    public void close() throws IOException {
	if (writer != null) {
	    writer.close();
	} else {
	    reader.close();
	}
    }
}
