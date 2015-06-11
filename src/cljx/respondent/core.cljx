(ns respondent.core
        (:refer-clojure :exclude [filter map deliver])

#+clj
	(:import [clojure.lang IDeref])

#+clj
	(:require [clojure.core.async :as async
	   :refer [go go-loop chan <! >! timeout
	   	   map> filter> close! mult tap untap]])

#+cljs
	(:require [cljs.core.async :as async
	   :refer [chan <! >! timeout
	   	   map> filter> close! mult tap untap]])


#+cljs
	(:require-macros [respondent.core :refer [behavior]]
		 [cljs.core.async.macros :refer [go go-loop]]))

;;--- we start here now -------------------------

(defprotocol IBehavior
   (sample [b interval]
   "Turns this Behavior into an EventStream from the sampled values at the given interval"))


(defprotocol IEventStream
   (map [s f]
   	"Returns a new stream containing the result of applying of to the values in s")
    (filter [s pred]
       "Returns a new stream containing the items from s for which pred returns true")
     (flatmap [s f]
        "Takes a function f from values in s to a new EventStream.
         Returns an EventStream containing values from all underlying streams combined.")
      (deliver [s value]
        "Delivers a value to the stream s")
       (completed? [s]
         "Returns true if this stream has stopped emitting values. False otherwise."))


(defprotocol IObservable
   (subscribe [obs f] "Register a callback to be invoked when the underlying source changes. Returns a token the subscriber can use to cancel the subscription."))


(defprotocol IToken
   (dispose [tk]
   "Called when the subscriber isn't interested in receiving more items"))

(deftype Token [ch]
   IToken
   (dispose [_]
     (close! ch)))

(defn event-stream
      "Creates and returns a new event stream. You can optionally provide an existing core.async channel as the source for the new stream"
      ([]
	(event-stream (chan)))
      ([ch]
	(let [multiple (mult ch)
	      completed (atom false)]
	    (EventStream. ch multiple completed))))

