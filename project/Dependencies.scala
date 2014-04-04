/*
 * Copyright (c) 2013-2014 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
import sbt._

object Dependencies {

  object V {
    val scalaTest = "2.0"
    val slf4j = "1.7.6"
    val logback = "1.1.1"

    val akka = "2.3.1"
    val spray = "1.3.1"
    val typesafeLogging = "1.0.1"

    val json4s = "3.2.7"

    val bouncycastle = "1.50"
    val commonsCodec = "1.9"
  }

  object Libraries {
    val scalaTest= "org.scalatest"           %% "scalatest"                % V.scalaTest % "test"
    val typesafeLogging = "com.typesafe"     %% "scalalogging-slf4j"       % V.typesafeLogging
    val logback = "ch.qos.logback"           % "logback-classic"          % V.logback % "test"

    val sprayClient = "io.spray"             % "spray-client"     % V.spray
    val sprayRouting = "io.spray"            % "spray-routing"     % V.spray
    val sprayTestKit = "io.spray"            % "spray-testkit" % V.spray % "test"

    val json4sNative = "org.json4s"          %% "json4s-native" % V.json4s
    val json4sJackon = "org.json4s"          %% "json4s-jackson" % V.json4s

    val akka = "com.typesafe.akka"           %% "akka-actor"    % V.akka

    val bouncycastle = "org.bouncycastle" % "bcprov-jdk15on" % V.bouncycastle
    val bouncycastleOpenSSL = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncycastle
    val commonsCodec = "commons-codec" % "commons-codec" % V.commonsCodec
  }

  import Libraries._ 

  val chefClient = Seq(scalaTest, typesafeLogging, logback, sprayClient, sprayRouting, json4sNative, json4sJackon, akka, sprayTestKit, bouncycastle, bouncycastleOpenSSL, commonsCodec)
}
