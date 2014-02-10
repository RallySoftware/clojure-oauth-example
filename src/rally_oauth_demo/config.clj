(ns rally-oauth-demo.config)


(defn config
  "Returns the value of the given environment variable, or the
  `if-not-set` value."
  ([name]
     (config name nil))
  ([name if-not-set]
     (or (System/getenv name) if-not-set)))


(defn require!
  "Throws an exception if any of the given names is not set as an
  environment variable."
  [& env-var-names]
  (doseq [name env-var-names]
    (when-not (config name)
      (throw (Exception. (str "Environment variable " name " must be set."))))))
