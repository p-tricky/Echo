<?php

use App\Pic;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder {

	/**
	 * Run the database seeds.
	 *
	 * @return void
	 */
	public function run() {
		Model::unguard();

		$this->call('PicDataSeeder');
	}

}

class PicDataSeeder extends Seeder {

	public function run() {
		DB::table('pics')->delete();

		Pic::create(array('aws_key' => 'file1.jpg', 'location' => '42.4424,-85.6479'));
		Pic::create(array('aws_key' => 'file2.jpg', 'location' => '42.4426,-85.6476'));
		Pic::create(array('aws_key' => 'file3.jpg', 'location' => '42.4424,-85.6481'));
		Pic::create(array('aws_key' => 'file4.jpg', 'location' => '42.4425,-85.6478'));
		Pic::create(array('aws_key' => 'file5.jpg', 'location' => '42.4423,-85.6479'));
	}

}
