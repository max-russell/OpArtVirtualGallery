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
