(defproject com.esotericsoftware.kryo/kryo "2.01"
  :description "Fast, efficient Java serialization"
  :java-source-path "src"
  :jvm-opts ["-Xmx768m" "-server"]
  :junit [["classes"]]
  :junit-options {:fork "off" :haltonfailure "on"}
  :javac-source-path [["src"] ["test"]]
  :dependencies [[com.googlecode/reflectasm "1.01"]
                 [com.googlecode/minlog "1.2"]]
  :dev-dependencies [[junit "4.8.2"]
                     [lein-javac "1.3.0"]
                     [lein-junit "1.0.0"]])
