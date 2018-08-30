package akka.persistence.couchbase

import akka.actor.ExtendedActorSystem
import akka.persistence.query.{ReadJournalProvider, javadsl}
import akka.persistence.query.scaladsl.ReadJournal
import com.typesafe.config.Config

class CouchbaseReadJournalProvider(as: ExtendedActorSystem, config: Config, configPath: String) extends ReadJournalProvider {
  override def scaladslReadJournal(): ReadJournal = new CouchbaseReadJournal(as, config, configPath)
  // FIXME todo
  override def javadslReadJournal(): javadsl.ReadJournal = new javadsl.ReadJournal {
  }
}
