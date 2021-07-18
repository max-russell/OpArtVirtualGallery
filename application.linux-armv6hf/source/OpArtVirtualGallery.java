import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.sound.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class OpArtVirtualGallery extends PApplet {



//These constants can be adjusted for better performance. Lowering 'SZ' may make the sketch run better on low-spec machines. 
final int FRAME_RATE = 25;
final int SZ = 800; //This is the size of a unit of length in the sketch, by which all other measurements in the gallery are derived. It is also the size of the viewport.

//These control the duration of the various actions in the sketch (in seconds)
final float STEP_DURATION = 0.5f; //Time taken to step forwards or backwards
final float TURN_DURATION = 0.5f; //Time take to turn 90 degress
final float PAUSE_DURATION = 0.5f; //Pause duration just before starting the art effect.
final float ART_EFFECT_DURATION = 2.0f; //Time taken for the art effect.

//These constants are derived from the above.
final int HALF_SZ = SZ / 2;
final int STEP_FRAMES = (int)(STEP_DURATION * FRAME_RATE);
final int TURN_FRAMES = (int)(TURN_DURATION * FRAME_RATE);
final int ART_EFFECT_FRAMES = (int)(ART_EFFECT_DURATION * FRAME_RATE);
final int PAUSE_FRAMES = (int)(PAUSE_DURATION * FRAME_RATE);
final int TEXT_SIZE = max(9, SZ / 42);

//Global variables storing the user's position and orientation, frame counters, and perspective/camera settings.
Direction dir = Direction.NORTH;
float posX = 0;
float posZ = 0;
float cameraTilt = 0;
float cameraRoll = 0;
float cameraPan = 0;
float fieldOfView = HALF_PI;
float viewportAspect = 1.0f;
float forwardPositionModifier = 0;
float angle = 0, destAngle = 0;
float animateCounter = 0;
int stepCounter = 0;
int artEffectCounter = 0;
int pauseCounter = 0;
Artwork currentArtEffect = null;
boolean actionInProgress = false;
boolean displayDiagnostics = false;

//The Map object in which the gallery's layout data is stored.
Map map;

//Textures used as a basis for constructing some of the art pieces
PImage[] textureCircles = new PImage[4];
PImage textureWaves;
PImage textureStripes;

SoundFile sndMove, sndInaction, sndArt, sndTransform;

public void settings()
{
  size(SZ, SZ, P3D);
}

public void setup()
{
  frameRate(FRAME_RATE);
  camera(0,0,0,0,0,-HALF_SZ,0,1,0);
  textureMode(NORMAL);
  noStroke();
  
  PGraphics g;
  
  //Create the textures for the animating circles used to mark the position of an artwork
  for(int n = 0; n < 4; n++)
  {
    g = createGraphics(SZ,SZ);
    g.beginDraw();
    g.noStroke();
    g.background(0);
    g.fill(255);
    g.ellipse(SZ / 2, SZ / 2, SZ/10 * (n+1), SZ/10 * (n+1));
    g.fill(0);
    g.ellipse(SZ / 2, SZ / 2, SZ/10 * n, SZ/10 * n);
    g.endDraw();
    textureCircles[n] = g;
  }
  
  //Create the 'Stripes' texture image. This is used in 'Opening' and 'Climax'.
  g = createGraphics(SZ,SZ);
  g.beginDraw();
  g.noStroke();
  g.background(255);
  g.fill(0);
  for(int n = 0; n < 16; n++)
  {
    g.rect(n * SZ / 16, 0, SZ/32, SZ);
  }
  g.endDraw();
  textureStripes = g;
  
  //Create the 'Waves' texture image. This is used in 'Intake' and 'Fall'.
  g = createGraphics(SZ,SZ, P3D);
  g.beginDraw();
  for(int n = -SZ/8; n <= SZ + SZ/8; n += SZ/4)
  {
    drawWave(g, n, SZ/8);
  }
  g.endDraw();
  textureWaves = g;
  
  //Initalise the map object, which stores the layout of the gallery.
  map = new Map();
  posX = map.startX;
  posZ = map.startY;  
  
  sndMove = new SoundFile(this, "movement.wav");
  sndInaction = new SoundFile(this, "no_action.wav");
  sndArt = new SoundFile(this, "FX01.aif");
  sndTransform = new SoundFile(this, "sfx_sounds_interaction8.wav");
}

public void draw()
{ 
  background(255);
  
  doStepMovement();
  
  pushMatrix();
  
  float cameraZ = (height/2.0f) / tan(fieldOfView/2.0f);
  perspective(fieldOfView, viewportAspect, cameraZ/10.0f, cameraZ*200.0f);
  
  //Matrix transformations based on the user's position and orientation.
  rotateX(cameraTilt);
  rotateZ(cameraRoll);
  rotateY(cameraPan);
  translate(0,0,-forwardPositionModifier);
  rotateY(dir.getAngle() + angle);
  translate(-(posX * SZ), 0, -(posZ * SZ));
  
  if (currentArtEffect instanceof ArtMovementInSquares && pauseCounter == 0)
  {
    //This is a fix for 'Movement in Squares', to hide other geometry while the art effect is running.
    //The rest of the gallery gets in the way otherwise, due to the extreme camera movement. 
    currentArtEffect.makeGeometry(g);
  }
  else
  { 
    //Make the walls, floors and ceilings of the gallery
    makeGallery(g);
 
    //Make each art piece, consisting of the wall image, and its corresponding geometry in the gallery.
    for(Artwork aw: map.artworks)
    {
      g.pushMatrix();
      g.translate(SZ * aw.x, 0, SZ * aw.y);
      rotateY(-aw.facingDir.getAngle());
      makeWall(g, aw.flatImage);
      g.popMatrix();
      
      g.pushMatrix();
      aw.makeGeometry(g);
      g.popMatrix();
    }
  }
  
  popMatrix();
  
  if (displayDiagnostics)
  {
    drawDiagnostics();
  }
} 

public void makeGallery(PGraphics g)
{
  //Reads the layout data in the map object and constructs the basic walls, floors and ceilings of the gallery.
  
  g.pushMatrix();
  for(int y = 0; y < map.getHeight(); y++)
  {
    g.pushMatrix();
    for(int x = 0; x < map.getWidth(); x++)
    {
      if (map.floorAt(x,y) > 0)
      {
        g.fill(0);
        //Floor
        g.beginShape();
        if (map.floorAt(x,y) == 2) 
        {
          g.texture(textureWaves);
        }
        else if (map.floorAt(x,y) == 3) 
        {
          g.texture(textureCircles[(int)(animateCounter / (FRAME_RATE * 0.3f)) % 4]);
        }
        
        g.vertex(-HALF_SZ, HALF_SZ, -HALF_SZ, 0, 1);
        g.vertex(HALF_SZ, HALF_SZ, -HALF_SZ, 0, 0);
        g.vertex(HALF_SZ, HALF_SZ, HALF_SZ, 1, 0);
        g.vertex(-HALF_SZ, HALF_SZ, HALF_SZ, 1, 1);
        g.endShape(CLOSE);
        
        //Ceiling
        g.beginShape();
        g.vertex(-HALF_SZ, -HALF_SZ, -HALF_SZ,0,0);
        g.vertex(HALF_SZ, -HALF_SZ, -HALF_SZ,1,0);
        g.vertex(HALF_SZ, -HALF_SZ, HALF_SZ,1,1);
        g.vertex(-HALF_SZ, -HALF_SZ, HALF_SZ,0,1);
        g.endShape(CLOSE);
      }
      
      //Walls in each cardinal direction.
      Direction w = Direction.NORTH;
      for(int d = 0; d < 4; d++)
      {
        if (map.wallAt(x,y,w))
        {
          makeWall(g, null);
        }
        rotateY(-HALF_PI);
        w = w.clockwise();
      }
      
      g.translate(SZ,0,0);
    }
    g.popMatrix();
    g.translate(0,0,SZ);
  }
  g.popMatrix();
  
  g.pushMatrix();
  g.translate(map.startX * SZ, 0, map.startY * SZ);
  makeInstructions(g);
  g.popMatrix(); 
}

public void makeWall(PGraphics g, PImage wall)
{
  g.translate(0,0, -HALF_SZ);
  fill(255);
  if (wall == null)
  {
    rect(-HALF_SZ, -HALF_SZ, SZ, SZ);
  }
  else
  {
    g.image(wall, -HALF_SZ, -HALF_SZ);
  }
  g.translate(0,0, HALF_SZ);
}

public void makeInstructions(PGraphics g)
{
  //The instructions are on the north wall at the start position, and the first thing the player sees when the sketch begins.
  g.pushMatrix();
  
  //First construct the 'plaque' on which the instruction text is written.
  int w1 = (int)(SZ / 2.5f);
  int h1 = SZ / 3;
  int w2 = (int)(w1 - SZ/16);
  int h2 = (int)(h1 - SZ/16);
  int d = SZ / 24;
  
  g.translate(0,0, -HALF_SZ);
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(-w1, -h1, 0, 0.25f, 0.25f);
  g.vertex(w1, -h1, 0, 0.25f, 0);
  g.vertex(w2, -h2, d, 0, 0.25f);
  g.vertex(-w2, -h2, d, 0, 0);
  g.endShape();
  
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(-w2, h2, d, 0, 0);
  g.vertex(w2, h2, d, 0, 0.25f);
  g.vertex(w1, h1, 0, 0.25f, 0);
  g.vertex(-w1, h1, 0, 0.25f, 0.25f);
  g.endShape();  
  
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(-w1, h1, 0, 0.25f, 0.25f);
  g.vertex(-w1, -h1, 0, 0.25f, 0);
  g.vertex(-w2, -h2, d, 0, 0.25f);
  g.vertex(-w2, h2, d, 0, 0);
  g.endShape();    
  
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(w2, h2, d, 0, 0);
  g.vertex(w2, -h2, d, 0, 0.25f);
  g.vertex(w1, -h1, 0, 0.25f, 0);
  g.vertex(w1, h1, 0, 0.25f, 0.25f);
  g.endShape();  
  
  g.beginShape();
  g.vertex(-w2, -h2, d, 0, 0);
  g.vertex(w2, -h2, d, 1, 0);
  g.vertex(w2, h2, d, 1, 1);
  g.vertex(-w2, h2, d, 0, 1);
  g.endShape();
  
  //Now write the text itself onto the plaque.
  
  String introMessage =
  "WELCOME TO THE\n" +
  "BRIDGET RILEY VIRTUAL GALLERY\n\n" +
  "Use the arrow keys to turn left or right\n" +
  "and to step forwards or backwards. Or\n" +
  "alternatively, click the edges of the screen.\n\n" +
  "Step on the circles on the floor and face\n" +
  "the wall to see an artistic transformation.\n\n" +
  "Now turn LEFT to begin.";
  
  g.translate(0,0,d + SZ/128);
  g.fill(0);
  g.textSize(TEXT_SIZE);
  g.textAlign(CENTER);
  g.text(introMessage,0, - TEXT_SIZE * 7);
  g.translate(0,0, HALF_SZ);
  g.popMatrix();
}

public void drawWave(PGraphics g, int x, int thickness)
{ 
  g.noStroke();
  g.fill(0);
  g.beginShape();
  g.vertex(x, 0);
  g.bezierVertex(x - HALF_SZ, HALF_SZ, x+HALF_SZ, HALF_SZ, x, SZ);
  g.vertex(x, SZ);
  g.vertex(x + thickness, SZ);
  g.bezierVertex(x + HALF_SZ + thickness, HALF_SZ, x - SZ/2+thickness, HALF_SZ, x+thickness, 0);
  g.vertex(x+thickness, 0);
  g.endShape();
}

public void setStandardPerspective(PGraphics g)
{
  float cameraZ = HALF_SZ / tan(HALF_PI/2.0f);
  g.perspective(HALF_PI, 1.0f, cameraZ/10.0f, cameraZ*200.0f);  
}

public void drawDiagnostics()
{
  setStandardPerspective(g);
  
  translate(0,0, -HALF_SZ);
  fill(255,0,0);
  textAlign(LEFT);
  
  textSize(TEXT_SIZE);
  text((int)posX + ", " + (int)posZ + " (" + dir.getName() + ")", 0, -HALF_SZ + TEXT_SIZE);
  text("Tilt: " + cameraTilt + "   Pan: " + cameraPan + "   Roll: " + cameraRoll, 0, -HALF_SZ + 2 * TEXT_SIZE);
  text("Aspect: " + viewportAspect, 0, -HALF_SZ + 3 * TEXT_SIZE);  
  text("fov: " + fieldOfView, 0, -HALF_SZ + 4 * TEXT_SIZE);
  text("forward: " + forwardPositionModifier, 0, -HALF_SZ + 5 * TEXT_SIZE);
}
class ArtMovementInSquares extends Artwork
{
  public ArtMovementInSquares(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createMovementInSquaresImage();
    teleportX = 3;
    teleportY = 2;
    teleportDir = Direction.NORTH;
  }

  public PImage createMovementInSquaresImage()
  {
    PGraphics g = createGraphics(SZ,SZ, P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    g.noStroke();
    g.background(255);
    float f = 0.001f * HALF_PI;
    float cameraZ = (height/2.0f) / tan(f/2.0f);
    g.perspective(f, 1.0f, cameraZ/10.0f, cameraZ*200.0f);
    float modZ = -(HALF_SZ / tan(f / 2) - HALF_SZ);
    g.translate(0, 0, modZ);
    makeTwoCylinders(g);
    g.endDraw();
    return g;
  }
  
  public void doMovement()
  {
    //We gradually increase the field of view as the animation proceeds.
    fieldOfView = ((float)(ART_EFFECT_FRAMES - artEffectCounter) / ART_EFFECT_FRAMES) * (PI / 2);
    
    //The tan function is used to calculate how far back to move the camera to compensate for the narrowed field of view.
    forwardPositionModifier = (HALF_SZ / tan(fieldOfView / 2) - HALF_SZ);
  }
  
  public void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    makeTwoCylinders(g);
    animateCounter++;  
  }
  
  public void makeTwoCylinders(PGraphics g)
  {
    float rotateAmount = -animateCounter / 100;
    
    g.pushMatrix();
    g.translate(-HALF_SZ,0,-SZ);
    g.rotateY(-rotateAmount);
    makeCylinder(g);
    g.rotateY(rotateAmount);
    g.translate(SZ, 0, 0);
    g.rotateY(rotateAmount);
    makeCylinder(g);
    g.rotateY(-rotateAmount);
    g.popMatrix();
  }
  
  public void makeCylinder(PGraphics g)
  {
    //A cylinder consists of rectangular segments.
    
    float SEGMENTS = 32; //How many segments there are around the cylinder
    float SEG_ANGLE = TWO_PI / SEGMENTS;
    float VSEGMENTS = 12; //How many segments there are vertically from floor to ceiling.
    float VSEG_DISTANCE = SZ / VSEGMENTS;
    
    for(int n = 0; n < SEGMENTS; n++)
    {
      float x1 = cos(n * SEG_ANGLE) * HALF_SZ;
      float z1 = sin(n * SEG_ANGLE) * HALF_SZ;
      float x2 = cos((n+1) * SEG_ANGLE) * HALF_SZ;
      float z2 = sin((n+1) * SEG_ANGLE) * HALF_SZ;
      
      for (int y = 0; y < VSEGMENTS; y++)
      {
        g.fill(((y + n) % 2) * 255); //Rectangles alternate between black and white.
        g.beginShape();
        float yfrom = -HALF_SZ + y * VSEG_DISTANCE;
        float yto = yfrom + VSEG_DISTANCE;
        g.vertex(x1, yfrom, z1);
        g.vertex(x2, yfrom, z2);
        g.vertex(x2, yto, z2);
        g.vertex(x1, yto, z1);
        g.endShape();
      }
    }
  }
}
class ArtIntake extends Artwork
{
  float tiltAmount = -1.1f;
  float rollAmount = -0.2f;
  float panAmount = 0.49f;
  
  public ArtIntake(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createIntakeImage();
    teleportX = 2;
    teleportY = 4;
    teleportDir = Direction.EAST;
  }

  public PImage createIntakeImage()
  { 
    PGraphics g = createGraphics(SZ,SZ, P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    setStandardPerspective(g);
    g.rotateX(tiltAmount); 
    g.rotateZ(rollAmount); 
    g.rotateY(panAmount); 
    g.translate(-SZ, 0, 0);
    g.noStroke();
    g.textureMode(NORMAL);
    makeRoom(g, 3, 4, textureWaves);
    g.endDraw();
    return g;
  }
  
  public void doMovement()
  {
    cameraTilt = (artEffectCounter / (float)ART_EFFECT_FRAMES) * tiltAmount; 
    cameraRoll = (artEffectCounter / (float)ART_EFFECT_FRAMES) * rollAmount;
    cameraPan = (artEffectCounter / (float)ART_EFFECT_FRAMES) * panAmount;
  }
  
  public void makeGeometry(PGraphics g)
  {
    //Making the geometry for 'Intake' is handled in the main 'makeGallery' routine
  }
  
  public void makeRoom(PGraphics g, int tiles_to_the_right, int tiles_ahead, PImage floor)
  {
    g.pushMatrix();
    
    for(int m = 0; m < tiles_to_the_right; m++)
    {
      g.pushMatrix();
      
      for(int n = 0; n < tiles_ahead; n++)
      {
        g.fill(0);
        g.beginShape();
        if (floor != null) g.texture(floor);
        g.vertex(-HALF_SZ, HALF_SZ, -HALF_SZ, 0, 0);
        g.vertex(HALF_SZ, HALF_SZ, -HALF_SZ, 1, 0);
        g.vertex(HALF_SZ, HALF_SZ, HALF_SZ, 1, 1);
        g.vertex(-HALF_SZ, HALF_SZ, HALF_SZ, 0, 1);
        g.endShape(CLOSE);
        g.translate(0,0,-SZ);
      }
      g.popMatrix();
      g.translate(SZ,0,0);
    }
    
    g.popMatrix();
  }
}
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
  
  public PImage createOpeningImage()
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
  
  public void doMovement()
  {
    forwardPositionModifier = SZ -((float)(ART_EFFECT_FRAMES - artEffectCounter) * (float)SZ/ART_EFFECT_FRAMES);
    
    if (artEffectCounter == 0)
    {
      posX = 10;
      posZ = 10;
      dir = Direction.EAST;
    }
    
  }
  
  public void makeOpening(PGraphics g)
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
  
  public void makeGeometry(PGraphics g)
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
class ArtFall extends Artwork
{
  public ArtFall(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createFallImage();
    teleportX = 2;
    teleportY = 3;
    teleportDir = Direction.WEST;
  }
  
  public PImage createFallImage()
  {
    PGraphics g = createGraphics(SZ,SZ);
    g.beginDraw();
    makeFall(g, 0);
    g.endDraw();
    return g;
  }
  
  public void doMovement()
  {
    forwardPositionModifier = -SZ + ((float)(ART_EFFECT_FRAMES - artEffectCounter) * (float)SZ/ART_EFFECT_FRAMES);
  }
  
  public void makeFall(PGraphics g, float phase)
  {
    g.pushMatrix();
    
    //In the setup phase during pre-rendering a 3D renderer is not needed.
    if (g.is3D())
      g.translate(-HALF_SZ,-HALF_SZ, -HALF_SZ);
    
    g.scale(1.0f/16,1.0f/16);
    
    //The waves texture is drawn in 16 rows, the amount to stretch each row is determined with the 'sin' function to provide
    //a smooth wave effect. By tying the sine curve's phase to the sketch's animation counter, the waves seem to undulate 
    //across the screen with each frame.
    
    for(int y = 0; y < 16; y++)
    {
      float h = (sin(y / 16.0f * TWO_PI + phase) + 1.00f);
      if (h < 0.1f) h = 0.1f;
      g.scale(1,  h);
      for(int x = 0; x < 16; x++)
      {
        g.image(textureWaves, SZ * x, 0);
      }
      g.translate(0,SZ);
      g.scale(1,1/h);
     
    } 
    g.popMatrix();
  }
  
  public void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    
    g.translate(-SZ,0,-SZ);
    for(int n = 0; n < 3; n++)
    {
      makeFall(g, animateCounter / 25);
      g.translate(SZ, 0, 0);
    }
  }
}
class ArtHero extends Artwork
{
  int SEGMENTS = 26;
  
  //The size of the triangular segments of the artwork are hardcoded to roughly match Riley's original composition.
  float[] segmentProportionsBottom = {0.001f, 0.054f, 0.106f, 0.168f, 0.230f, 0.296f, 0.366f, 0.442f, 0.520f, 0.606f, 0.692f, 0.788f, 0.888f, 1.0f};
  float[] segmentProportionsTop = {0.001f, 0.043f, 0.121f, 0.235f, 0.388f, 0.632f, 0.694f, 0.749f, 0.799f, 0.848f, 0.895f, 0.935f, 0.973f, 1.0f};
  
  public ArtHero(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createHeroImage();
    teleportX = 10;
    teleportY = 13;
    teleportDir = Direction.WEST;
  }
  
  public PImage createHeroImage()
  {
    PGraphics g = createGraphics(SZ,SZ,P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    float cameraZ = HALF_SZ / tan(HALF_PI/2.0f);
    g.perspective(HALF_PI, 1.0f/(SZ / SEGMENTS), cameraZ/10.0f, cameraZ*200.0f); 
    g.noStroke();
    g.background(255);
    makeHero(g);
    g.endDraw();
    return g;    
  }
  
  public void doMovement()
  {
    int n = SZ / SEGMENTS;
    viewportAspect = 1 / (((float)artEffectCounter/ART_EFFECT_FRAMES) * (n-1) + 1);
  }
  
  public void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    makeHero(g);
  }

  public void makeHero(PGraphics g)
  {    
      g.pushMatrix();
    
      g.translate(-HALF_SZ, HALF_SZ, -HALF_SZ);
                             
      g.fill(0);
      for(int n = (SEGMENTS/2)-1; n >= 0; n--)
      { 
        //This draws the segments onto the floor, which will occupy the bottom half of the 2D image.
        float d1 = (HALF_SZ / (HALF_SZ * segmentProportionsBottom[n+1])) * HALF_SZ - HALF_SZ;
        float d2 = (HALF_SZ / (HALF_SZ * segmentProportionsBottom[n])) * HALF_SZ - HALF_SZ;
        float dd1 = HALF_SZ - (d1 + HALF_SZ) / (SZ/(float)SEGMENTS);
        
        g.beginShape();
        g.vertex(dd1, 0,-d1);
        g.vertex(HALF_SZ, 0,-d2);
        g.vertex(SZ - dd1,0,-d1);
        g.endShape(CLOSE);
        
        //Now draw the segments onto the ceiling, which will occupy the top half of the 2D image.
        d1 = (HALF_SZ / (HALF_SZ * segmentProportionsTop[n+1])) * HALF_SZ - HALF_SZ;
        d2 = (HALF_SZ / (HALF_SZ * segmentProportionsTop[n])) * HALF_SZ - HALF_SZ;
        dd1 = HALF_SZ - (d1 + HALF_SZ) / (SZ/(float)SEGMENTS);      
        
        g.beginShape();
        g.vertex(dd1, -SZ,-d1);
        g.vertex(HALF_SZ, -SZ,-d2);
        g.vertex(SZ - dd1,-SZ,-d1);
        g.endShape(CLOSE);
      }
      
      g.popMatrix();
  }
}
class ArtClimax extends Artwork
{  
  int SEGMENTS = 64;
  float SEG_ANGLE = PI / SEGMENTS;
  int VSEGMENTS = 10;
  float VSEG_DISTANCE = SZ / VSEGMENTS;
  
  float[][] curve_x = new float[SEGMENTS+4][2];
  float[][] curve_z = new float[SEGMENTS+4][2]; 
  
  public ArtClimax(int x, int y, Direction dir)
  {
    super(x, y, dir);
    
    //Rather than calculating the vertex positions in the half-cylinder on the fly every frame, they are pre-calculated during setup.
    for(int n = 0; n < SEGMENTS+4; n++)
    {
      float q = (cos(((float)n/SEGMENTS) * PI + PI) + 1) / 2 * PI;
      curve_z[n][0] = -sin(q) * (HALF_SZ * 3);
      curve_x[n][0] = cos(q) * (HALF_SZ * 3);
      curve_z[n][1] = -sin(q) * (HALF_SZ * 2.9f);
      curve_x[n][1] = cos(q) * (HALF_SZ * 2.9f);    
    }
    
    flatImage = createClimaxImage();
    teleportX = 8;
    teleportY = 11;
    teleportDir = Direction.NORTH;
  }
  
  public PImage createClimaxImage()
  {
    PGraphics g = createGraphics(SZ,SZ, P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    float cameraZ = HALF_SZ / tan(HALF_PI/2.0f);
    g.perspective(HALF_PI, 0.4f, cameraZ/10.0f, cameraZ*200.0f);  
    g.rotateZ(HALF_PI);
    g.translate(0,0,-HALF_SZ); 
    g.noStroke();
    g.background(0);
    g.textureMode(NORMAL);
    makeClimaxCurve(g);
    g.endDraw();
    return g;
  }

  public void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    g.translate(0,0,-HALF_SZ); 
    makeClimaxCurve(g);
  }
  
  public void makeClimaxCurve(PGraphics g)
  {    
    for(int n = 0; n < SEGMENTS-1; n+=4)
    { 
      for (int y = 0; y < VSEGMENTS; y++)
      {
        float yfrom = -HALF_SZ + y * VSEG_DISTANCE;
        float yto = yfrom + VSEG_DISTANCE;
        
        g.beginShape();
        g.texture(textureStripes);
        if (y % 2 == 0) //The segments protrude alternately inward and outward from top to bottom.
        {                        
          g.vertex(curve_x[n][0], yfrom, curve_z[n][0],0,0);
          g.vertex(curve_x[n+4][0], yfrom, curve_z[n+4][0],1,0);
          g.vertex(curve_x[n+5][1], yto, curve_z[n+5][1],1,1); 
          g.vertex(curve_x[n+1][1], yto, curve_z[n+1][1],0,1);
        }
        else
        {
          g.vertex(curve_x[n+1][1], yfrom, curve_z[n+1][1],0,0);
          g.vertex(curve_x[n+5][1], yfrom, curve_z[n+5][1],1,0);
          g.vertex(curve_x[n+4][0], yto, curve_z[n+4][0],1,1); 
          g.vertex(curve_x[n][0], yto, curve_z[n][0],0,1);
        }
        g.endShape();
      }  
    }    
  }
  
  public void doMovement()
  {
    viewportAspect = 1 - (artEffectCounter / (float)ART_EFFECT_FRAMES) * 0.6f; 
    cameraRoll = (artEffectCounter / (float)ART_EFFECT_FRAMES) * HALF_PI;
  }
}
//Having an abstract base class for the artwork, makes it much simpler to expand the sketch with more artworks. Each descendent
//class just needs to override the constructor and two other methods: 'makeGeometry', which places the geometry pertaining to the artwork in the gallery, and
//'doMovement' which controls how the camera settings animate when the art effect is triggered.

abstract class Artwork
{
  int x, y;
  int teleportX, teleportY;
  Direction facingDir;
  Direction teleportDir;
  protected PImage flatImage;
  
  public Artwork(int x, int y, Direction dir)
  {
    this.x = x;
    this.y = y;
    this.facingDir = dir;
    this.teleportX = x;
    this.teleportY = y;
    this.teleportDir = dir;
  }
  
  public abstract void makeGeometry(PGraphics g);
  
  public abstract void doMovement();
}
enum Direction 
{
  NORTH(0, "North"), 
  EAST(HALF_PI, "East"), 
  SOUTH(PI, "South"), 
  WEST(HALF_PI * 3, "West");
  
  private float angle;
  private String name;
  
  private Direction(float angle, String name)
  {
    this.angle = angle;
    this.name = name;
  }
  
  public Direction clockwise()
  {
    if (this == NORTH) return EAST;
    else if(this == EAST) return SOUTH;
    else if(this == SOUTH) return WEST;
    else return NORTH;
  }
  
  public Direction antiClockwise()
  {
    if (this == NORTH) return WEST;
    else if(this == EAST) return NORTH;
    else if(this == SOUTH) return EAST;
    else return SOUTH;
  }
  
  public float getAngle() {return angle;}
  public String getName() {return name;}
}
public void keyPressed()
{
  if (!actionInProgress && pauseCounter == 0)
  {
    if (keyCode == RIGHT)
    {
      startRightTurn();
    }
    else if (keyCode == LEFT)
    {
      startLeftTurn();
    }
    else if (keyCode == UP)
    {
      startForwardStep();
    }
    else if (keyCode == DOWN)
    {
      startBackwardStep();
    }    
    else if (keyCode == ENTER)
    {
      displayDiagnostics = !displayDiagnostics;
    }
  }
}

public void mouseClicked()
{
  if (!actionInProgress && pauseCounter == 0)
  {
    boolean topRightArea = mouseX > mouseY;
    boolean topLeftArea = (SZ - mouseX) > mouseY;
    
    if (topRightArea && topLeftArea)
    {
      startForwardStep();
    }
    else if (!topRightArea && !topLeftArea)
    {
      startBackwardStep();
    }
    else if (topLeftArea && !topRightArea)
    {
      startLeftTurn();
    }
    else if (topRightArea && !topLeftArea)
    {
      startRightTurn();
    } 
  }
}

public void startForwardStep()
{
  stepCounter = STEP_FRAMES;
  actionInProgress = true;
  sndMove.play();
}

public void startBackwardStep()
{
  stepCounter = -STEP_FRAMES;
  actionInProgress = true;
  sndMove.play();
}

public void startLeftTurn()
{
  destAngle = -HALF_PI;
  actionInProgress = true;
  sndMove.play();
}

public void startRightTurn()
{
  destAngle = HALF_PI;
  actionInProgress = true;
  sndMove.play();
}
class Map
{ 
  // The gallery layout is described in a string for ease of input. This is converted to a 2-dimensional array in the
  // map object's constructor, as this is more efficient for the sketch to read from during runtime.
  // Had the gallery been larger and more complex geometrically it would likely have been better to import it
  // from a file made with an external editor, such as Blender.
  
  private int mapWidth = 12, mapHeight = 15;
  private String mapSetupData =
  "            " +
  "##---       " +
  "w#####      " +
  "w#mmmm###E# " +
  "w#mmmm #### " +
  "##mmm# F### " +
  " #     ###D " +
  " ##A####B## " +
  " C####    # " +
  " #####S####-" +
  "   #   ####-" +
  "  #########-" +
  "  ######### " +
  "----------# " +
  "            ";
  
  //This function is used to read the string in the constructor method.
  private char mapCharAt(int x, int y)
  {
    if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) return ' ';
    return mapSetupData.charAt(y * mapWidth + x);
  }
  private static final int NORTH_BITMASK = 16, EAST_BITMASK = 32, SOUTH_BITMASK = 64, WEST_BITMASK = 128;
  private short[][] mapData = new short[mapWidth][mapHeight];
  
  public int startX, startY;
  public ArrayList<Artwork> artworks = new ArrayList<Artwork>();
  
  public Map()
  {
    //The map data is built from the String setup data when the Map object is created.
    
    for(int y = 0; y < mapHeight; y++)
    {
      for(int x = 0; x < mapWidth; x++)
      {
        switch(mapCharAt(x, y))
        {
          case '#': //A standard walkable tile
            mapData[x][y] = 1;
            if (mapCharAt(x,y-1) == ' ') mapData[x][y] += NORTH_BITMASK;
            if (mapCharAt(x+1,y) == ' ') mapData[x][y] += EAST_BITMASK;
            if (mapCharAt(x,y+1) == ' ') mapData[x][y] += SOUTH_BITMASK;
            if (mapCharAt(x-1,y) == ' ') mapData[x][y] += WEST_BITMASK;
            break;
          case 'm': //Tile with wave pattern floor.
            mapData[x][y] = 2;
            if (mapCharAt(x,y-1) == ' ') mapData[x][y] += NORTH_BITMASK;
            if (mapCharAt(x+1,y) == ' ') mapData[x][y] += EAST_BITMASK;
            if (mapCharAt(x,y+1) == ' ') mapData[x][y] += SOUTH_BITMASK;
            if (mapCharAt(x-1,y) == ' ') mapData[x][y] += WEST_BITMASK;
            break;
          case 'w': //Tile with west side wall only.
            mapData[x][y] += WEST_BITMASK;
            break;
          case 'S': //Player start position
            mapData[x][y] = 1;
            mapData[x][y] += SOUTH_BITMASK + EAST_BITMASK + NORTH_BITMASK;
            startX = x;
            startY = y;
            break;
          case 'A':
            mapData[x][y] = 3;
            artworks.add(new ArtMovementInSquares(x, y, Direction.NORTH));
            break;
          case 'B':
            mapData[x][y] = 3;
            artworks.add(new ArtIntake(x, y, Direction.SOUTH));
            break;
          case 'C':
            mapData[x][y] = 3;
            artworks.add(new ArtOpening(x, y, Direction.WEST)); 
            break;
          case 'D':
            mapData[x][y] = 3;
            artworks.add(new ArtFall(x, y, Direction.EAST));
            break;
          case 'E':
            mapData[x][y] = 3;
            artworks.add(new ArtHero(x, y, Direction.NORTH)); 
            break;    
          case 'F':
            mapData[x][y] = 3;
            artworks.add(new ArtClimax(x, y, Direction.WEST)); 
        }
      }
    }
  }
  
  public int getWidth() {return mapWidth;}
  public int getHeight() {return mapHeight;}
  
  public int floorAt(int x, int y)
  {
    return mapData[x][y] & 15;
  }
  
  public boolean wallAt(int x, int y, Direction d)
  {
    switch(d)
    {
      case NORTH:
        return (mapData[x][y] & NORTH_BITMASK) != 0;
      case EAST:
        return (mapData[x][y] & EAST_BITMASK) != 0;
      case SOUTH:
        return (mapData[x][y] & SOUTH_BITMASK) != 0;
      case WEST:
        return (mapData[x][y] & WEST_BITMASK) != 0;
    }
    return false;
  } 
}
public void doStepMovement()
{
  if (pauseCounter > 0) //Handling the pause before an art effect begins
  {
    pauseCounter--;
    if (pauseCounter == 0)
    {
      artEffectCounter = ART_EFFECT_FRAMES;
      actionInProgress = true;
      sndTransform.play();
      posX = currentArtEffect.teleportX;
      posZ = currentArtEffect.teleportY;
      dir = currentArtEffect.teleportDir;
      animateCounter = 0;    
    }
    else return;
  }   
  
  if (actionInProgress)
  {
    if (destAngle > angle) //Turning right
    {
      angle += HALF_PI / TURN_FRAMES;
      if (angle >= destAngle)
      { 
        dir = dir.clockwise();
        angle = destAngle = 0;
        actionInProgress = false;
      }
    }
    else if (destAngle < angle) //Turning left
    {
      angle -= HALF_PI / TURN_FRAMES;
      if (angle <= destAngle)
      {
        dir = dir.antiClockwise();
        destAngle = angle = 0;
        actionInProgress = false;
      }
    }
    else if (stepCounter > 0) //Stepping forwards
    {
      stepCounter--;
      
      forwardPositionModifier = -(STEP_FRAMES - stepCounter) * SZ/STEP_FRAMES;
      if (stepCounter == 0)
      {
        if (dir == Direction.SOUTH) posZ++;
        if (dir == Direction.NORTH) posZ--;
        if (dir == Direction.EAST) posX++;
        if (dir == Direction.WEST) posX--;        
       
        forwardPositionModifier = 0;
        actionInProgress = false;
      }
    }
    else if (stepCounter < 0) //Stepping backwards
    {
      stepCounter++;
      forwardPositionModifier = (STEP_FRAMES + stepCounter) * SZ/STEP_FRAMES;
      if (stepCounter == 0)
      {
        if (dir == Direction.SOUTH) posZ--;
        if (dir == Direction.NORTH) posZ++;
        if (dir == Direction.EAST) posX--;
        if (dir == Direction.WEST) posX++;        
       
        forwardPositionModifier = 0;
        actionInProgress = false;
      }      
    }
    
    else if (currentArtEffect != null) //Updating an art effect
    { 
      artEffectCounter--;
      
      currentArtEffect.doMovement();
      
      if (artEffectCounter == 0)
      {
        actionInProgress = false;
        forwardPositionModifier = 0;
        currentArtEffect = null;
      }
    }
    
    if (actionInProgress == false) //Check for triggering an art effect
    {
      for(Artwork artwork: map.artworks)
      {
        if (posX == artwork.x && posZ == artwork.y && dir == artwork.facingDir)
        {
          pauseCounter = PAUSE_FRAMES;
          currentArtEffect = artwork;
          sndArt.play();
        }
      }   
    }
    
  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "OpArtVirtualGallery" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
