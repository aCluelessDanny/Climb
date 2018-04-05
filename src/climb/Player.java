
package climb;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

public class Player extends Object {
    private Level lvl;
    private Rectangle box, ledgeBox;
    private int velX, velY;
    private boolean facingRight;
    private boolean grounded, climbing, walledL, walledR;
    private Boundary climbableBound;
    
    public Player(int x, int y, int width, int height, Level lvl) {
	super(x, y, width, height);
	this.lvl = lvl;
	
	box = new Rectangle(x, y, width, height);
	ledgeBox = new Rectangle(x + width / 2, y + height / 5, 1, 3 * height / 5);
	
	velX = 0; velY = 0;
	facingRight = true;
	grounded = false; climbing = false;
	walledL = false; walledR = false;
    }
    
    private boolean canClimb() {
	if (climbableBound == null) {
	    return false;
	}
	return ledgeBox.y <= climbableBound.getWallL().y + climbableBound.getWallL().height &&
		ledgeBox.y + ledgeBox.height >= climbableBound.getWallL().y;
    }
    
    public void ledgeClimb() {
	if (getX() < climbableBound.x) {
	    setX(climbableBound.x + 1);
	} else {
	    setX(climbableBound.x + climbableBound.width - getWidth() - 1);
	}
	setY(climbableBound.y - getHeight());
	velY = 0;
	
	walledL = false;
	walledR = false;
	climbing = false;
    }
    
    public void wallJump() {
	if (facingRight) {
	    velX = -10;
	    walledR = false;
	} else {
	    velX = 10;
	    walledL = false;
	}
	velY = -12;
	climbing = false;
	grounded = false;
    }
    
    public void updateBoxes() {
	box.x = getX();
	box.y = getY();
	if (walledL) {
	    ledgeBox.x = getX() - 1;
	} else if (walledR) {
	    ledgeBox.x = getX() + getWidth() - 1;
	} else {
	    ledgeBox.x = getX() + getWidth() / 2;
	}
	ledgeBox.y = getY() + getHeight() / 5;
    }
    
    @Override
    public void tick() {
	boolean up = lvl.getKeyMan().pressed(KeyEvent.VK_UP),
		down = lvl.getKeyMan().pressed(KeyEvent.VK_DOWN),
		left = lvl.getKeyMan().pressed(KeyEvent.VK_LEFT),
		right = lvl.getKeyMan().pressed(KeyEvent.VK_RIGHT),
		keyC = lvl.getKeyMan().pressed(KeyEvent.VK_C);
	grounded = false;
	boolean l, r, g;
	
	// Boundary collision detection
	for (Boundary b : lvl.getBounds()) {
	    l = false; r = false; g = false;

	    if (!(walledL || walledR) && box.intersects(b.getWallL())) {	    // If player touches the left wall
		climbableBound = b;
		if (velX > 0) {
		    velX = 0;
		    setX((int) b.getWallL().getX() - getWidth());
		}
		r = true;
	    } else if (!(walledL || walledR) && box.intersects(b.getWallR())) {	    // If player touches the right wall
		climbableBound = b;
		if (velX < 0) {
		    velX = 0;
		    setX((int) (b.getWallR().getX() + b.getWallR().getWidth()));
		}
		l = true;
	    }
	    if (box.intersects(b.getFloor())) {	    // If player touches the floor
		g = true;
	    } else if (box.intersects(b.getCeiling()) && !grounded && !climbing) {	// If player touches the ceiling
		velY = 0;
	    }
	    
	    if (g && l) {
		if (getX() < b.x + b.width) {
		    l = false;
		} else {
		    g = false;
		}
	    } else if (g && r) {
		if (getX() + getWidth() > b.x) {
		    r = false;
		} else {
		    g = false;
		}
	    }
	    
	    if (g) {
		setY((int) (b.getFloor().getY() - getHeight()) + 1);
		velY = 0;
	    }
	    
	    grounded = g ? true : grounded;
	    walledL = l ? true : walledL;
	    walledR = r ? true : walledR;
	    
	    if (grounded && (walledL || walledR)) {
		break;
	    }
	}
	
	if ((walledL || walledR) && canClimb()) {
	    if (keyC) {
		climbing = true;
	    } else if (climbing && !keyC) {
		climbing = false;
	    }
	} else {
	    walledL = false;
	    walledR = false;
	}
	
	if (climbing) {	    // Vertical movement (climbing)
	    if ((up && down) || (!up && !down)) {
		velY = 0;
	    } else if (up) {
		velY = -2;
		if (ledgeBox.y + ledgeBox.height + velY < climbableBound.getWallL().y) {
		    ledgeClimb();
		}
	    } else if (down && !grounded) {
		velY = 2;
		if (ledgeBox.y + velY > climbableBound.getWallL().y + climbableBound.getWallL().height) {
		    velY = 0;
		}
	    }
	} else {	    // Horizontal movement
	    if ((!left && !right) || (left && right)) {
		velX /= 2;
	    } else if (left && !walledL) {
		walledR = false;
		velX -= (velX > -6 ? 2 : 0);
	    } else if (right && !walledR) {
		walledL = false;
		velX += (velX < 6 ? 2 : 0);
	    }
	}
	
	if (grounded) {
	    if (velX > 0) {
		facingRight = true;
	    } else if (velX < 0) {
		facingRight = false;
	    }
	} else if (!climbing) {
	    if ((walledL && left) || (walledR && right)) {
		velY = (velY > 3 ? velY - 2 : velY + 1);
		facingRight = right;
	    } else if (velY < 8) {
		velY++;
	    }
	}
	
	if (lvl.getKeyMan().typed(KeyEvent.VK_SPACE)) {
	    if (grounded) {
		grounded = false;
		velY = -15;
	    } else if (walledL || walledR) {
		wallJump();
	    }
	}
	
	// Update position with velocity
	setX(getX() + velX);
	setY(getY() + velY);
	
	// Collision position update
	updateBoxes();
    }

    @Override
    public void render(Graphics g) {
	g.setColor(Color.white);
	g.drawRect(box.x, box.y, box.width, box.height);
	g.setColor(Color.magenta);
	g.drawRect(ledgeBox.x, ledgeBox.y, ledgeBox.width, ledgeBox.height);
    }
}
