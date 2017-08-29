# Changes

## 0.2.1 / 2017-08-29

* Improve internal implementation of callbacks mechanism
* Use Jetty HTTP server instead of Netty-based
* DirectoryConfigSource do not throw exceptions if can't find base directory

## 0.2.0 / 2017-08-21

* Enhance ConfigProperty API 
* Support Duration config type 
* Support typed lists config types 
* Support custom executors for property callbacks 

## 0.1.1 / 2017-08-17

* Change HTTP resource base path to `_config`
* Add shortcut methods to get property values from `ConfigRegistry`
* Add `addFisrtSource` and `addBeforeSource` methods to `ConfigRegistrySettings`

## 0.1.0 / 2017-08-15

* Initial release
