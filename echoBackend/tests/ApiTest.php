<?php

use Illuminate\Foundation\Testing\WithoutMiddleware;
use Illuminate\Foundation\Testing\DatabaseMigrations;
use Illuminate\Foundation\Testing\DatabaseTransactions;

class ApiTest extends TestCase
{
    use DatabaseTransactions;
    public function testPicStore()
    {
      $this->post('/pics', [ 
        'lat' => '42.42',
        'lon' => '47.47',
        'aws_key' => 'testApi' 
      ])->seeJson([ 
        'created' => 'true', 
      ]);
    }

    public function testGetPicsTakenNearMe()
    {
      $this->post('/pics', [ 
        'lat' => '42.42',
        'lon' => '47.47',
        'aws_key' => 'testApi' 
      ]);
      $this->get('/getPicsTakenNearMe?lat=42.42&lon=47.47')
      ->seeJson([
        'aws_key' => 'testApi', 
      ]);
    }

    public function testDeleteSuccess()
    {
      $this->post('/pics', [ 
        'lat' => '42.42',
        'lon' => '47.47',
        'aws_key' => 'testApi' 
      ]);
      $this->delete('/pics/testApi')
      ->seeJson([
        'success' => 'true', 
      ]);
    }

    public function testDeleteFail()
    {
      $this->delete('/pics/filedontexist.jpg')
      ->seeJson([
        'success' => 'false', 
      ]);
    }
}
