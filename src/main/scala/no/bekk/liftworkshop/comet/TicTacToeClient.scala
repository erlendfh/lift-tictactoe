package no.bekk.liftworkshop.comet

import controller._
import model.Game
import net.liftweb._
import http._
import js._
import JsCmds._
import JE._
import SHtml._
import xml.{Text, NodeSeq}

class TicTacToeClient extends CometActor with CometListenee {
  var username: String = ""
  var currentGame: Game = null
  
  protected def registerWith = TicTacToeServer

  def render = {
    if(username.equals("")) {
      renderForm(defaultXml)
    } else if(currentGame != null) {
      println("rendering game board")
      renderGame(defaultXml)
    } else {
      renderLobby(defaultXml)
    }
  }

  def renderForm(defaultXml: NodeSeq) = {
    ajaxForm(
      bind("login", chooseTemplate("client", "login", defaultXml),
        "username" -> text("", username = _),
        "submit" -> submit("Log in", () => {})) ++
      hidden(() => {
        TicTacToeServer ! AddUser(username, this)
      })
    )
  }

  def renderLobby(defaultXml: NodeSeq) = {
    // Fetch active users from Lobby, and filter out current user
    println("Updating user list for user " + username)
    val users = TicTacToeServer.users.keySet.filter(user => !user.equals(username)).toSeq

    val list = users.flatMap(user =>
      bind("list", chooseTemplate("lobby", "list", defaultXml),
        "username" -> Text(user),
        "challengeLink" -> a(() => createGame(user), Text("Challenge"))
      )
    )

    bind("lobby", chooseTemplate("client", "lobby", defaultXml),
      "list" -> list,
      "currentUser" -> Text(username)
    )
  }

  def renderGame(defaultXml: NodeSeq) = {
    bind("game", chooseTemplate("client", "game", defaultXml),
      "board" -> currentGame.cells.zipWithIndex.flatMap({case (value, index) =>
        bind("boardCell", chooseTemplate("game", "board", defaultXml),
          "value" -> a(() => makeMove(index), Text(value))
        )
      }),
      "playerA" -> Text(currentGame.playerA),
      "playerB" -> Text(currentGame.playerB),
      "endGameLink" -> a(() => endCurrentGame, Text("End game"))
    )
  }

  def createGame(opponent: String) = {
    TicTacToeServer ! CreateGame(username, opponent)
    Noop
  }

  def makeMove(cell: Int) = {
    TicTacToeServer ! MakeMove(username, currentGame, cell)
    Noop
  }

  def endCurrentGame = {
    TicTacToeServer ! EndGame(currentGame)
    Noop
  }


  override def lowPriority = {
    case UpdateClient =>
      reRender(false)

    case JoinGame(game: Game) => {
      currentGame = game
      reRender(false)
    }

    case LeaveGame(game: Game) => {
      currentGame = null
      reRender(false)
    }
  }
}
