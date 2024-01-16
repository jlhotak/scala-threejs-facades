package typings.three.addons.loaders

import org.scalajs.dom.{ProgressEvent, ErrorEvent}
import scala.scalajs.js
import js.annotation.*


import typings.three.loaders.*
import typings.three.cameras.*
import typings.three.animation.*
import typings.three.objects.*

@js.native
trait GLTF extends js.Object:
  var animations: js.Array[AnimationClip]
  var asset: Any//Copyright
  var cameras: js.Array[Camera]
  var parser: GLTFParser
  var scene: Group
  var scenes: js.Array[Group]
  var userData: Any

@js.native
@JSImport("three/examples/jsm/loaders/GLTFLoader", "GLTFLoader")
class GLTFLoader extends Loader:
  def this(manager: js.UndefOr[LoadingManager] = js.undefined) = this()
  def load(url: String, onLoad: js.UndefOr[js.Function1[GLTF, Unit]] = js.undefined, onProgress: js.UndefOr[js.Function1[ProgressEvent,Unit]] = js.undefined, onError: js.UndefOr[js.Function1[ErrorEvent,Unit]] = js.undefined): Unit= js.native


@js.native
@JSImport("three", "GLTFParser")
class GLTFParser extends js.Object:
  var json: Any = js.native

  def parse(onLoad: js.UndefOr[js.Function1[GLTF, Unit]] = js.undefined, onError: js.UndefOr[js.Function1[ErrorEvent,Unit]] = js.undefined ): Unit = js.native


