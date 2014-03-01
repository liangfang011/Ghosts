package com.liangfang.ghosts.graphics;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface PieceImages extends ClientBundle {
	@Source("images/pieces/BACK.png")
	ImageResource back();
	
	@Source("images/pieces/GOOD.png")
	ImageResource good();
	
	@Source("images/pieces/EVIL.png")
	ImageResource evil();
}
