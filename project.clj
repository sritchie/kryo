(defproject com.twitter/kryo "2.04"
  :description "Fast, efficient Java serialization"
  :java-source-path "src"
  :jvm-opts ["-Xmx768m" "-server"]
  :junit [["classes"]]
  :junit-options {:fork "off" :haltonfailure "on"}
  :javac-source-path [["src"] ["test"]]
  :repositories {"conjars" "http://conjars.org/repo/"}
  :dependencies [[com.esotericsoftware.reflectasm/reflectasm "1.02"]
                 [org.objenesis/objenesis "1.2"]
                 [com.googlecode/minlog "1.2"]]
  :dev-dependencies [[junit "4.8.2"]
                     [lein-javac "1.3.0"]
                     [lein-junit "1.0.0"]])
