/** 
 * Window and graphics functionality for pCG. Includes Turtle Graphics.
 * Window contents are remembered and refreshed when the window is obscured.
 * <p>
 * Originally created as part of a library for a "J0" compiler developed 
 * in a course taken with Martin Odersky in 1999 at Uni SA.
 *
 * David Benn, October 2000
 */

package cgp.runtime.newtypes;

import cgp.runtime.ListType;
import cgp.runtime.NumberType;
import cgp.runtime.StringType;
import cgp.runtime.Type;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class Window extends Type {
    // Instance fields.
    private GCPFrame f; // singleton frame for this window object
    
    // Constructors.
    public Window() {
	setType("window");
    }
    
    // -- Methods --
    
    public String toString() {
	return "a window";
    }    
    
    // pCG operations for this class.
    
    /**
     * Open a graphics window.
     */
    public void open(StringType title, 
		     NumberType l, NumberType t, 
		     NumberType w, NumberType h) {
	int left, top, width, height;

	left = (int)l.getValue();
	top = (int)t.getValue();
	width = (int)w.getValue();
	height = (int)h.getValue();

	if (f == null) {
	    f = new GCPFrame(title.getValue(), left, top, width, height);
	}
    }
    
    /**
     * Close a graphics window.
     */
    public void close() {
	if (f != null) {
	    f.setVisible(false);
	    f.dispose();
	    f = null;
	}
    }

    /*
     * Check that a RGB value is reasonable.
     */
    private boolean colorComponentOK(int n) {
	return n >=0 && n <= 255;
    }
    
    
    /**
     * Set the colour in the CGP graphics window.
     */
    public void setColor(ListType rgb) {
	LinkedList list = rgb.getValue();
	int[] nums = new int[list.size()];

	for (int i=0;i<list.size();i++) {
	    Type x = (Type)list.get(i);
	    if (x instanceof NumberType) {
		nums[i] = (int)((NumberType)x).getValue();
	    } else {
		return; // should generate an error instead
	    }
	}

	setColor(nums);
    }

    /**
     * Set the colour in the CGP graphics window.
     * Internal method.
     */
    private void setColor(int[] rgb) {
	if (f != null) {
	    if (rgb.length == 3) {
		if (colorComponentOK(rgb[0]) && colorComponentOK(rgb[1]) &&
		    colorComponentOK(rgb[2])) {
		    f.setColor(rgb);
		}
	    }      
	}
    }
    
    /**
     * Draw a line in the CGP graphics window.
     */
    public void drawLine(NumberType x1, NumberType y1,
			 NumberType x2, NumberType y2) {
	if (f != null) {
	    f.drawLine((int)x1.getValue(), (int)y1.getValue(),
		       (int)x2.getValue(), (int)y2.getValue());
	}
    }
    
    /**
     * Draw a string in the CGP graphics window.
     */
    public void drawText(StringType s, NumberType x, NumberType y) {
	if (f != null) {
	    f.drawText(s.getValue(), (int)x.getValue(), (int)y.getValue());
	}
    }

    /**
     * Draw an image in the CGP graphics window.
     */
    public void drawImage(StringType path, NumberType x, NumberType y) {
	if (f != null) {
	    f.drawImage(path.getValue(), (int)x.getValue(), (int)y.getValue());
	}
    }
    
    /**
     * Turtle graphics.
     * Move to the specified location in the CGP graphics window.
     */
    public void moveTo(NumberType x, NumberType y) {
	if (f != null) {
	    f.moveTo((int)x.getValue(), (int)y.getValue());
	}    
    }
    
    /**
     * Turtle graphics.
     * Draw a line to the specified location in the CGP graphics window.
     */
    public void lineTo(NumberType x, NumberType y) {
	if (f != null) {
	    f.lineTo((int)x.getValue(), (int)y.getValue());
	}    
    }
    
    /**
     * Turtle graphics.
     * Turn the turtle by the specified number of degrees (left = negative).
     */
    public void turn(NumberType degrees) {
	if (f != null) {
	    f.turn((int)degrees.getValue());
	}    
    }
    
    /**
     * Turtle graphics.
     * Walk the turtle in the current direction specfiied by turn().
     * If a negative value is specified, the turtle will move in reverse.
     */
    public void walk(NumberType steps) {
	if (f != null) {
	    f.walk((int)steps.getValue());
	}    
    }
}

// ----- Frame classes -----

/**
 * This class provides simple drawing and Turtle Graphics capabilities.
 */
class GCPFrame extends Frame {
    // Instance fields.
    private int x,y; // turtle's position
    private int angle; // turtle's angle
    private CGPCanvas c;
    private Color color;
    
    // Constructors.
    GCPFrame(String title, int left, int top, int width, int height) {
	super(title);
	x = width/2;
	y = height/2;
	angle = 270; // point north
	c = new CGPCanvas();
	this.add(c);
	this.setBounds(left,top,width,height);
	this.addWindowListener(new GCPFrameListener());
	this.show();
	color = c.getGraphics().getColor();
    }
  
    // ImageObserver methods.
    /*
    public boolean imageUpdate(Image img, int infoflags, 
			       int x, int y, int width, int height) {
	// Do nothing.
	//c.getGraphics().drawImage(im,x,y,this); // make ImageObserver null?
	return true;
    }
    */
    // --- General gfx methods ---
    
    public void setColor(int[] rgb) {
	Color color = new Color(rgb[0],rgb[1],rgb[2]);
	if (color != null) {
	    c.setForeground(color);
	    this.color = color;
	}
    }
    
    public void drawLine(int x1, int y1, int x2, int y2) {
	c.getGraphics().drawLine(x1,y1,x2,y2);
	c.gfxObjs.add(new Line(x1,y1,x2,y2,color));
    }
    
    public void drawText(String s, int x, int y) {
	c.getGraphics().drawString(s,x,y);
	c.gfxObjs.add(new Text(s,x,y,color));
    }
    
    public void drawImage(String path, int x, int y) {
	try {
	    URL url = new URL(path);
	    ImageProducer ip = (ImageProducer)url.getContent();
	    Image im = createImage(ip); // this method is in Component
  	    c.getGraphics().drawImage(im,x,y,c);
	    // Must do this next step since the data won't be loaded
	    // immediately, so we'll want paint() to be called soon.
	    c.gfxObjs.add(new Pic(im,c,x,y,color));
	} catch (MalformedURLException e) {
	    // return an error code for URL creation failure?
	} catch (IOException e) {
	    // return an error code for getContent() failure?
	}
    }


    // --- Turtle Graphics methods ---
    
    public void moveTo(int x, int y) {
	this.x = x;
	this.y = y;
    }
    
    public void lineTo(int x, int y) {
	drawLine(this.x,this.y,x,y);
	this.x = x;
	this.y = y;
    }
    
    public void turn(int degrees) {
	angle += degrees;
    }
    
    public void walk(int steps) {
	double theta = degs2rads(angle);
	int newX = (int)Math.round(this.x + steps*Math.cos(theta));
	int newY = (int)Math.round(this.y + steps*Math.sin(theta));
	drawLine(this.x,this.y,newX,newY);
	this.x = newX;
	this.y = newY;
    }
    
    private double degs2rads(int angle) {
	return angle/57.295779;
    }
}

// Use inner classes below instead!

class GCPFrameListener extends WindowAdapter {
    // Close and dispose the window upon a close request.
    public void windowClosing(WindowEvent e) {
	java.awt.Window w = e.getWindow();
	w.setVisible(false);
	w.dispose();
    }
}

class CGPCanvas extends Canvas {
    public LinkedList gfxObjs; // the graphics objects on this canvas
    
    CGPCanvas() {
	gfxObjs = new LinkedList();
    }
    
    public void paint(Graphics g) {
	// Draw all stored graphics objects when the canvas needs repainting.
	// Inefficient but simple.
	Iterator it = gfxObjs.iterator();
	GfxObj gobj;
	while (it.hasNext()) {
	    gobj = (GfxObj)it.next();
	    gobj.render(g);
	}
    }
}

// --- Graphics Objects ---

abstract class GfxObj {
    private Color color;

    GfxObj(Color color) {
	this.color = color;
    }

    void render(Graphics g) {
	g.setColor(color);
    }
}

class Line extends GfxObj {
    private int x1,y1,x2,y2;

    Line(int x1, int y1, int x2, int y2, Color color) {
	super(color);
	this.x1 = x1;
	this.y1 = y1;
	this.x2 = x2;
	this.y2 = y2;
    }
    
    void render(Graphics g) {
	super.render(g);
	g.drawLine(x1, y1, x2, y2);
    }
}

class Text extends GfxObj {
    private String s;
    private int x,y;

    Text(String s, int x, int y, Color color) {
	super(color);
	this.s = s;
	this.x = x;
	this.y = y;
    }

    void render(Graphics g) {
	super.render(g);
	g.drawString(s, x, y);
    }
}

class Pic extends GfxObj {
    private Image im;
    private ImageObserver obs;
    private int x,y;

    Pic(Image im, ImageObserver obs, int x, int y, Color color) {
	super(color);
	this.im = im;
	this.obs = obs;
	this.x = x;
	this.y = y;
    }

    void render(Graphics g) {
	super.render(g);
	g.drawImage(im, x, y, obs);
    }
}

