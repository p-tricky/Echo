<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

use App\Http\Requests;
use App\Http\Controllers\Controller;
use App\Pic;
use Storage;
use Log;
use Response;
use Illuminate\Database\Eloquent\ModelNotFoundException;

class PicController extends Controller
{
    /**
     * Display a listing of the resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function index()
    {
      $theArray = Pic::all();
      $jsonArray = $theArray->toJson();
      return $jsonArray;
    }

    public function getPicsTakenNearMe(Request $request)
    {
        $lat = $request->input('lat');
        $lon = $request->input('lon');
        $location = $lat.",".$lon;
        $pics = Pic::distance(0.1, $location)->limit(10)->get();
        return Response::json($pics);
    }

    /**
     * Store a newly created resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\Response
     */
    public function store(Request $request)
    {
        $lat = $request->input('lat');
        $lon = $request->input('lon');
        $pic = new Pic;
        $location = $lat.",".$lon;
        $pic->setLocationAttribute($location);
        $pic['aws_key'] = $request->input('aws_key');
        $pic->save();
        $retVal = array('created' => 'true');
        return json_encode($retVal);
    }

    /**
     * Display the specified resource.
     *
     * @param  string  $key
     * @return \Illuminate\Http\Response
     */
    public function show($key)
    {
      $disk = Storage::disk('s3');
      $echo = $disk->get($key);
      return Response::make($echo, 200)->header('content-type', 'jpg');

    }

    /**
     * Update the specified resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function update(Request $request, $id)
    {
    }

    /**
     * Remove the specified resource from storage.
     *
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function destroy($aws_key)
    {
      $statusCode = 200;
      Log::info($aws_key);
      $retVal = array('success' => 'true');
      try {
        $pic = Pic::where('aws_key', '=', $aws_key)->firstOrFail();
        $pic->delete();
      } catch (ModelNotFoundException $e) {
        $retVal['success'] = 'false';
        $statusCode = 404;
        Log::error("Error getting delete file from echo server: " . $e->getMessage());
      }
      return response()->json($retVal, $statusCode);
    }
}                                                                                                                                   
