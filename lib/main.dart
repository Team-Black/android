import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:screenshot/screenshot.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PolyStream',
      theme: ThemeData(
        primarySwatch: Colors.green,
      ),
      home: MyHomePage(title: 'PolyStream'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = const MethodChannel('ivt.black/stream_controller');
  bool isLoading = false;
  int currentDevice;
  bool streaming = false;

  int _counter = 0;
  File _imageFile;
  Socket socket;
  Timer streamTimer;

  //Create an instance of ScreenshotController
  ScreenshotController screenshotController = ScreenshotController();

  @override
  Widget build(BuildContext context) {
    double width = MediaQuery.of(context).size.width;
    double height = MediaQuery.of(context).size.height;
    return Screenshot(
      controller: screenshotController,
      child: Stack(children: <Widget>[
        Scaffold(
          appBar: AppBar(
            backgroundColor: const Color(0xFF1B6225),
            title: Text(widget.title),
          ),
          body: Column(
            //mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              AnimatedContainer(
                duration: Duration(milliseconds: 300),
                height: currentDevice != null ? 70 : height / 2,
                width: width,
                decoration: BoxDecoration(
                    color: const Color(0xFF1B6225),
                    borderRadius: BorderRadius.circular(8)),
                margin: EdgeInsets.all(10),
                padding: const EdgeInsets.all(5.0),
                child: Container(
                  color: Colors.white,
                  child: currentDevice != null
                      ? ListTile(
                          title: Text('Device $currentDevice'),
                          trailing: IconButton(
                            iconSize: 32,
                            icon: Icon(
                              Icons.cancel,
                              color: Colors.red,
                            ),
                            onPressed: () {
                              setState(() {
                                currentDevice = null;
                              });
                            },
                          ),
                        )
                      : ListView(
                          children: List.generate(
                              10,
                              (index) => FlatButton(
                                  child: Text('Device $index'),
                                  onPressed: () => chooseDevice(index))),
                        ),
                ),
              ),
            ],
          ),
          drawer: Drawer(
            child: ListView(
              children: <Widget>[
                DrawerHeader(
                  child: Text(
                    'PolyStream',
                    style: Theme.of(context).textTheme.display1,
                  ),
                ),
                ListTile(
                  title: Text('Main page'),
                  onTap: () {},
                  selected: true,
                ),
                ListTile(
                  title: Text('Settings'),
                  onTap: () {},
                ),
                ListTile(
                  title: Text('Help'),
                  onTap: () {},
                ),
              ],
            ),
          ),
          floatingActionButton: Padding(
            padding: EdgeInsets.zero,
            child: Row(
              mainAxisSize: MainAxisSize.max,
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                currentDevice != null
                    ? FloatingActionButton(
                        onPressed: startStream,
                        tooltip: 'Start Stream',
                        child: Icon(streaming
                            ? Icons.pause_circle_filled
                            : Icons.play_circle_outline),
                      )
                    : Container(),
                FloatingActionButton(
                  isExtended: false,
                  child: Text('Connect to server'),
                  onPressed: makeScreenshot,
                )
              ],
            ),
          ), // This trailing comma makes auto-formatting nicer for build methods.
        ),
        if (isLoading)
          Container(
            height: height,
            width: width,
            color: Colors.black.withOpacity(0.3),
            child: Center(
              child: CircularProgressIndicator(),
            ),
          )
      ]),
    );
  }

  Future<bool> chooseDevice(int index) async {
    setState(() {
      isLoading = true;
    });

    await Future.delayed(Duration(seconds: 1));
    setState(() {
      currentDevice = index;
      isLoading = false;
    });
    print('Connect to device $index');
    return true;
  }

  Future startStream() async {
    if (!streaming) {
      try {
        print('START RECORDING');
        final String result = await platform.invokeMethod('start', {'g': 'g'});
        print('RESULT:' + result);
        setState(() {
          streaming = !streaming;
        });
      } on PlatformException catch (e) {
        print("Failed to send event: '${e.message}'.");
      } catch (e) {
        print(e);
      }
    } else {
      try {
        print('STOP RECORDING');
        final String result = await platform.invokeMethod('stop', {'g': 'g'});
        print('RESULT:' + result);
        setState(() {
          streaming = !streaming;
        });
      } on PlatformException catch (e) {
        print("Failed to send event: '${e.message}'.");
      } catch (e) {
        print(e);
      }
    }
  }

  void connectToServer(File image) async {
    // print('connected');
    try {
      socket = await Socket.connect('10.215.6.83', 3333);
      // listen to the received data event stream
//    socket.listen((List<int> event) {
//      print(utf8.decode(event));
//    });

      // send hello
      socket.add(image.readAsBytesSync());

      // wait 5 seconds
      //await Future.delayed(Duration(seconds: 10));

      // .. and close the socket
      socket.close();
    } catch (e) {
      streaming = false;
      streamTimer?.cancel();
      socket?.close();
    }
  }

  Future<Uint8List> networkImageToByte(
      {String path =
          'https://png.pngtree.com/png-clipart/20190515/original/pngtree-watercolor-border-png-image_3552532.jpg'}) async {
    HttpClient httpClient = HttpClient();
    var request = await httpClient.getUrl(Uri.parse(path));
    var response = await request.close();
    Uint8List bytes;
    print(bytes);
    try {
      Uint8List audioByte;
      String myPath = 'assets/test.jpg';
      _readFileByte(myPath).then((bytesData) {
        bytes = bytesData;
        //do your task here
      });
    } catch (e) {
      // if path invalid or not able to read
      print(e);
    }
    return bytes;
  }

  Future<Uint8List> _readFileByte(String filePath) async {
    Uri myUri = Uri.parse(filePath);
    return (await getFileData('assets/test.jpg')).buffer.asUint8List();
//    var file = File(data);
//    Uint8List bytes;
//    bytes = file.readAsBytesSync();
//    return bytes;
  }

  Future<ByteData> getFileData(String path) async {
    return await rootBundle.load(path);
  }

  makeScreenshot() async {
    if (streaming == false) {
      streaming = true;
      streamTimer?.cancel();
      try {
        streamTimer = Timer.periodic(Duration(milliseconds: 10), (t) {
          screenshotController.capture().then((File image) {
            try {
              connectToServer(image);
            } catch (e) {
              print(e);
              streaming = false;
              streamTimer?.cancel();
              socket?.close();
            }
          }).catchError((onError) {
            streaming = false;
            streamTimer?.cancel();
            socket?.close();
            print(onError);
          });
        });
      } catch (e) {
        streaming = false;
        streamTimer?.cancel();
        socket?.close();
        print(e);
      }
    } else {
      streaming = false;
      streamTimer?.cancel();
      socket?.close();
    }
    print('Stream $streaming');
  }
}
