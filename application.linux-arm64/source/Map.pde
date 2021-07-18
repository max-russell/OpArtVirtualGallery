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
