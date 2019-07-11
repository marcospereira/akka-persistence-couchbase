/*
 * Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package com.lightbend.lagom.scaladsl.persistence.couchbase

import akka.persistence.couchbase.CouchbaseBucketSetup
import com.lightbend.lagom.internal.persistence.ReadSideConfig
import com.lightbend.lagom.internal.scaladsl.persistence.couchbase.{
  CouchbasePersistentEntityRegistry,
  CouchbaseReadSideImpl,
  ScaladslCouchbaseOffsetStore
}
import com.lightbend.lagom.scaladsl.persistence.TestEntity.Evt
import com.lightbend.lagom.scaladsl.persistence._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Future

object CouchbaseReadSideSpec {

  val defaultConfig: Config = ConfigFactory.parseString("akka.loglevel = info")
}

class CouchbaseReadSideSpec
    extends CouchbasePersistenceSpec(CouchbaseReadSideSpec.defaultConfig, TestEntitySerializerRegistry)
    with AbstractReadSideSpec
    with CouchbaseBucketSetup {

  override protected lazy val persistentEntityRegistry = new CouchbasePersistentEntityRegistry(system)

  private lazy val offsetStore = new ScaladslCouchbaseOffsetStore(system, couchbaseSession, ReadSideConfig())
  private lazy val couchbaseReadSide = new CouchbaseReadSideImpl(system, couchbaseSession, offsetStore)

  override def processorFactory(): ReadSideProcessor[Evt] =
    new TestEntityReadSide.TestEntityReadSideProcessor(system, couchbaseReadSide)

  private lazy val readSide = new TestEntityReadSide(system, couchbaseSession)

  override def getAppendCount(id: String): Future[Long] = readSide.getAppendCount(id)

  override def afterAll(): Unit =
    super.afterAll()

}
