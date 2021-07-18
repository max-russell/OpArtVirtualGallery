class ArtOpening extends Artwork
{
  //The Opening artwork actually performs two teleports. The first moves the player out of the gallery while the camera flies through the
  //'opening', at the end of which the player is teleported back to the final position in the gallery.
  
  public ArtOpening(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createOpeningImage();
    teleportX = -10;
    teleportY = -10;
    teleportDir = Direction.NORTH;
  }
  
  PImage createOpeningImage()
  {
    PGraphics g = createGraphics(SZ,SZ, P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    setStandardPerspective(g); 
    g.noStroke();
    g.textureMode(NORMAL);
    makeOpening(g);
    g.endDraw();
    return g;
  }
  
  void doMovement()
  {
    forwardPositionModifier = SZ -((float)(ART_EFFECT_FRAMES - artEffectCounter) * (float)SZ/ART_EFFECT_FRAMES);
    
    if (artEffectCounter == 0)
    {
      posX = 10;
      posZ = 10;
      dir = Direction.EAST;
    }
    
  }
  
  void makeOpening(PGraphics g)
  {
    g.pushMatrix();
    g.translate(-HALF_SZ,-HALF_SZ, -HALF_SZ);
    
    g.beginShape();
    g.texture(textureStripes);
    g.vertex(0,0,0,0,0);
    g.vertex(SZ/2,0,0,1,0);
    g.vertex(0,SZ/2,0,0,1);
    g.endShape(CLOSE);
  
    g.beginShape();
    g.texture(textureStripes);
    g.vertex(SZ/2,0,0,0,0);
    g.vertex(SZ,0,0,1,0);
    g.vertex(SZ,SZ/2,0,1,1);
    g.endShape(CLOSE);
    
    g.beginShape();
    g.texture(textureStripes);
    g.vertex(0,SZ/2,0,0,0);
    g.vertex(0,SZ,0,0,1);
    g.vertex(SZ/2,SZ,0,1,1);
    g.endShape(CLOSE);
    
    g.beginShape();
    g.texture(textureStripes);
    g.vertex(SZ,SZ/2,0,1,0);
    g.vertex(SZ/2,SZ,0,0,1);
    g.vertex(SZ,SZ,0,1,1);
    g.endShape(CLOSE);
    
    g.translate(-SZ,-SZ,-SZ);
    
    for(int y =0; y < 3; y++)
    {
      for(int x = 0; x < 3; x++)
      {
        g.image(textureStripes, x * SZ, y * SZ);
        
      }
    }
    g.popMatrix();
  }
  
  void makeGeometry(PGraphics g)
  {
    if (currentArtEffect instanceof ArtOpening)
    {
      g.pushMatrix();
      g.translate(SZ * teleportX, 0, SZ * (teleportY + 1));
      g.rotateY(Direction.NORTH.getAngle());
      makeOpening(g);
      g.popMatrix(); 
    }
    g.translate(SZ * 10, 0, SZ * 11);
    g.rotateY(-Direction.EAST.getAngle());
    for(int n = 0; n < 3; n++)
    {
      makeWall(g, textureStripes);
      g.translate(-SZ, 0, 0);
    }
  }
}
