(ns adventures.clj.nextjdbc
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def ^:private db-spec {:dbtype "mariadb" :dbname "fishbowlDB" :username "singular" :password "Password"})


(defn -main [& args]
  (with-open [^HikariDataSource ds (connection/->pool HikariDataSource db-spec)]
    (jdbc/execute! ds ["SHOW DATABASES;"])))

(-main)

(comment
  (jdbc/execute!
   (jdbc/get-connection (jdbc/get-datasource db-spec))
   ['SHOW DATABASES;']))

